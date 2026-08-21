<#
.SYNOPSIS
    Prepara el entorno de despliegue de Vivace y publica el Worker y la app.

.DESCRIPTION
    Un solo script para las tres cosas que hacen falta para poner Vivace en
    produccion:

      1. -Setup        Comprueba e instala las herramientas: Node LTS, un JDK
                       17+, el SDK de Android (cmdline-tools, plataforma 35 y
                       build-tools), las dependencias del Worker y la sesion de
                       Cloudflare. Es idempotente: lo que ya esta, se deja.
      2. -InitBackend  Puesta en marcha del backend, solo la primera vez: crea
                       el bucket de R2, la base D1, aplica schema.sql y pide los
                       secretos AUTH_SECRET y SYNC_TOKEN.
      3. -Worker/-App  Despliegue: publica el Worker y/o compila el APK de
                       release y lo sube a R2 con su latest.json.

    Sin ningun parametro solo hace -Setup: publicar nunca ocurre por accidente.

    Los textos van sin acentos a proposito, para que se lean igual en la consola
    de Windows sea cual sea la pagina de codigos.

.PARAMETER Setup
    Instala y comprueba las herramientas. Es lo que se hace por defecto.

.PARAMETER InitBackend
    Crea bucket, base de datos y secretos. Solo la primera vez.

.PARAMETER Worker
    Publica el Worker en Cloudflare (npm run check + npm test + wrangler deploy).

.PARAMETER App
    Compila el APK de release firmado y lo sube a R2 junto con latest.json.

.PARAMETER All
    Equivale a -Setup -Worker -App.

.PARAMETER PublishCatalog
    Indexa en la base de datos las partituras que ya estaban en R2 antes del
    multiusuario y que por eso no salen en la web. No mueve ni reescribe ningun
    fichero: solo las registra a nombre del administrador que inicia sesion. Es
    idempotente, se puede repetir sin duplicar nada.

.PARAMETER CatalogVisibility
    Visibilidad con la que se indexan esas partituras: "public" (por defecto)
    las pone en el catalogo que ve cualquiera al entrar en la web; "private" las
    deja solo en "Mis partituras" del administrador. Solo afecta a las que se
    indexan en esta pasada.

.PARAMETER ListUsers
    Lista los usuarios de la base (email, rol y fecha de alta) consultando D1 con
    wrangler. No hace falta la cuenta de administrador: basta tu acceso a
    Cloudflare. Util para saber quien es el admin cuando el catalogo sale vacio.

.PARAMETER PromoteAdmin
    Cambia el rol de la cuenta indicada en -AdminEmail, escribiendo directamente
    en D1 con wrangler. Es la salida cuando nadie tiene la contrasena del admin
    original, y la via para nombrar editores.

.PARAMETER AdminEmail
    Email de la cuenta cuyo rol se cambia con -PromoteAdmin.

.PARAMETER Role
    Rol que se le da: "admin" (por defecto), "editor" o "user". El editor
    gestiona el catalogo, revisa propuestas y mantiene el diccionario de
    acordes; el admin ademas reparte roles.

.PARAMETER Categorize
    Propone una categoria musical para cada partitura a partir del artista y del
    titulo, y la aplica a las que no tengan ninguna. Primero ensena el recuento
    en seco y pregunta antes de escribir. Las reglas estan en
    worker/src/genres.js, a la vista para poder corregirlas.

.PARAMETER Overwrite
    Con -Categorize, repasa tambien las partituras que YA tienen categoria y las
    pisa con la que propongan las reglas. Sin esto solo se rellenan los huecos.

.PARAMETER ApplySchema
    Aplica schema.sql a la base de produccion. Todas las sentencias son
    CREATE ... IF NOT EXISTS, asi que repetirlo no rompe nada: es como se
    ponen al dia las tablas nuevas (versiones, propuestas) sin tocar los datos.

.PARAMETER Notes
    Texto de novedades que va en latest.json y que la app ensena en el aviso de
    actualizacion.

.PARAMETER BaseUrl
    URL publica del Worker (p. ej. https://guitarchords-sync.midominio.workers.dev).
    Si no se pasa, se intenta deducir de la salida de wrangler deploy. Sirve para
    comprobar que el versionCode que vas a publicar es mayor que el publicado.

.PARAMETER SkipTests
    Se salta los tests del Worker y de la app. Usalo solo si acabas de pasarlos.

.PARAMETER AllowDebugSigning
    Permite publicar un APK firmado con la clave de depuracion. Por defecto es un
    error: si la firma cambia, las actualizaciones no se instalan encima de la
    version que ya tiene la gente.

.PARAMETER Force
    No pregunta antes de publicar y no exige que suba el versionCode.

.EXAMPLE
    .\tools\deploy.ps1
    Deja el equipo listo para compilar y desplegar, sin publicar nada.

.EXAMPLE
    .\tools\deploy.ps1 -Worker
    Publica solo el Worker (web, API y panel).

.EXAMPLE
    .\tools\deploy.ps1 -App -Notes "Estilo Nocturno y afinador mas rapido"
    Compila el APK firmado, lo sube y actualiza latest.json.
#>
[CmdletBinding()]
param(
    [switch]$Setup,
    [switch]$InitBackend,
    [switch]$Worker,
    [switch]$App,
    [switch]$All,
    [switch]$PublishCatalog,
    [ValidateSet("public", "private")][string]$CatalogVisibility = "public",
    [switch]$ListUsers,
    [switch]$PromoteAdmin,
    [string]$AdminEmail = "",
    [ValidateSet("admin", "editor", "user")][string]$Role = "admin",
    [switch]$Categorize,
    [switch]$Overwrite,
    [switch]$ApplySchema,
    [string]$Notes = "",
    [string]$BaseUrl = "",
    [switch]$SkipTests,
    [switch]$AllowDebugSigning,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

# Version de las command-line tools del SDK de Android que se descarga si no hay
# ninguna. Cuando quede vieja se cambia aqui: sdkmanager se actualiza solo.
$CmdlineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$AndroidPlatform = "platforms;android-35"
$AndroidBuildTools = "build-tools;35.0.0"

$Root = Split-Path -Parent $PSScriptRoot
$WorkerDir = Join-Path $Root "worker"
$script:BaseUrlResuelta = ""

# ---------------------------------------------------------------- utilidades --

function Write-Step { param([string]$Text) Write-Host "`n=== $Text" -ForegroundColor Cyan }
function Write-Ok   { param([string]$Text) Write-Host "  OK  $Text" -ForegroundColor Green }
function Write-Info { param([string]$Text) Write-Host "  ..  $Text" -ForegroundColor DarkGray }
function Write-Note { param([string]$Text) Write-Host "  !   $Text" -ForegroundColor Yellow }
function Fail       { param([string]$Text) throw $Text }

# Ejecuta un programa y aborta si devuelve un codigo distinto de cero. No se
# redirige stderr a proposito: en Windows PowerShell 5.1 eso convierte cada
# linea de stderr en un error aunque el programa haya terminado bien.
function Invoke-Tool {
    param(
        [Parameter(Mandatory = $true)][string]$Exe,
        [string[]]$Arguments = @(),
        [string]$WorkDir = $Root,
        [switch]$Capture
    )
    Push-Location $WorkDir
    try {
        Write-Info ("{0} {1}" -f (Split-Path -Leaf $Exe), ($Arguments -join " "))
        $output = $null
        if ($Capture) {
            $output = & $Exe @Arguments
        } else {
            # Sin -Capture la salida del programa va directa a la consola.
            & $Exe @Arguments
        }
        if ($LASTEXITCODE -ne 0) {
            Fail ("Fallo '{0} {1}' (codigo {2})." -f (Split-Path -Leaf $Exe), ($Arguments -join " "), $LASTEXITCODE)
        }
        if ($Capture) { return $output }
    } finally {
        Pop-Location
    }
}

function Test-Tool {
    param([string]$Name)
    return ($null -ne (Get-Command $Name -ErrorAction SilentlyContinue))
}

function Confirm-Step {
    param([string]$Question)
    if ($Force) { return $true }
    $answer = Read-Host "$Question [s/N]"
    return ($answer -match '^[sSyY]')
}

# Lee la version mayor de un JDK de su fichero 'release'. Es mas fiable que
# parsear "java -version", que escribe por stderr.
function Get-JdkMajor {
    param([string]$JavaHome)
    $releaseFile = Join-Path $JavaHome "release"
    if (-not (Test-Path $releaseFile)) { return 0 }
    $line = Select-String -Path $releaseFile -Pattern '^JAVA_VERSION="([0-9]+)' -ErrorAction SilentlyContinue
    if ($null -eq $line) { return 0 }
    return [int]$line.Matches[0].Groups[1].Value
}

function Install-WithWinget {
    param([string]$Id, [string]$Nombre)
    if (-not (Test-Tool "winget")) {
        Fail "Falta $Nombre y no hay winget para instalarlo. Instalalo a mano y repite."
    }
    Write-Info "Instalando $Nombre con winget ($Id)..."
    & winget install --id $Id --exact --silent --accept-package-agreements --accept-source-agreements
    # winget devuelve 0x8A15002B (-1978335189) cuando el paquete ya estaba.
    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne -1978335189) {
        Fail "winget no pudo instalar $Nombre (codigo $LASTEXITCODE)."
    }
    # El PATH de esta sesion no se entera de lo recien instalado.
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
                [System.Environment]::GetEnvironmentVariable("Path", "User")
}

# ------------------------------------------------------------------- entorno --

function Initialize-Java {
    Write-Step "JDK 17 o superior"
    $candidatos = New-Object System.Collections.Generic.List[string]
    if ($env:JAVA_HOME) { $candidatos.Add($env:JAVA_HOME) }
    $candidatos.Add("C:\Program Files\Android\Android Studio\jbr")
    $candidatos.Add("C:\Program Files\Android\Android Studio Preview\jbr")
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($null -ne $java) { $candidatos.Add((Split-Path -Parent (Split-Path -Parent $java.Source))) }

    foreach ($c in $candidatos) {
        if ([string]::IsNullOrWhiteSpace($c)) { continue }
        if (-not (Test-Path $c)) { continue }
        $major = Get-JdkMajor $c
        if ($major -ge 17) {
            $env:JAVA_HOME = $c
            Write-Ok "JDK $major en $c"
            return
        }
    }

    Write-Note "No hay ningun JDK 17+; se instala Temurin 17."
    Install-WithWinget "EclipseAdoptium.Temurin.17.JDK" "Temurin 17"
    $encontrado = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
                  Select-Object -First 1
    if ($null -eq $encontrado) { Fail "Temurin 17 se instalo pero no aparece en C:\Program Files\Eclipse Adoptium." }
    $env:JAVA_HOME = $encontrado.FullName
    [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $env:JAVA_HOME, "User")
    Write-Ok "JDK 17 en $env:JAVA_HOME (JAVA_HOME guardado para el usuario)"
}

function Initialize-Node {
    Write-Step "Node.js y npm"
    if (Test-Tool "node") {
        $version = (& node -v).TrimStart("v")
        if ([int]($version.Split(".")[0]) -ge 18) {
            Write-Ok "Node $version"
            return
        }
        Write-Note "Node $version es demasiado viejo para wrangler; se instala la LTS."
    }
    Install-WithWinget "OpenJS.NodeJS.LTS" "Node.js LTS"
    if (-not (Test-Tool "node")) {
        Fail "Node se instalo pero no esta en el PATH. Abre una consola nueva y repite."
    }
    Write-Ok ("Node {0}" -f (& node -v))
}

function Get-AndroidSdkRoot {
    if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) { return $env:ANDROID_SDK_ROOT }
    $localProps = Join-Path $Root "local.properties"
    if (Test-Path $localProps) {
        $linea = Select-String -Path $localProps -Pattern '^\s*sdk\.dir\s*=\s*(.+)$' -ErrorAction SilentlyContinue
        if ($null -ne $linea) {
            $ruta = $linea.Matches[0].Groups[1].Value.Trim().Replace("\\", "\")
            if (Test-Path $ruta) { return $ruta }
        }
    }
    $porDefecto = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $porDefecto) { return $porDefecto }
    return $null
}

function Find-SdkManager {
    param([string]$SdkRoot)
    if ([string]::IsNullOrWhiteSpace($SdkRoot)) { return $null }
    $ruta = Join-Path $SdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
    if (Test-Path $ruta) { return $ruta }
    $carpeta = Join-Path $SdkRoot "cmdline-tools"
    if (Test-Path $carpeta) {
        $suelto = Get-ChildItem $carpeta -Recurse -Filter "sdkmanager.bat" -ErrorAction SilentlyContinue |
                  Select-Object -First 1
        if ($null -ne $suelto) { return $suelto.FullName }
    }
    return $null
}

function Initialize-AndroidSdk {
    Write-Step "SDK de Android (plataforma 35)"
    $sdkRoot = Get-AndroidSdkRoot
    if ($null -eq $sdkRoot) {
        $sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
        Write-Note "No hay SDK instalado; se pondra en $sdkRoot"
    }
    $sdkManager = Find-SdkManager $sdkRoot

    if ($null -eq $sdkManager) {
        Write-Info "Descargando las command-line tools del SDK..."
        $zip = Join-Path $env:TEMP "android-cmdline-tools.zip"
        $temporal = Join-Path $env:TEMP ("android-cmdline-tools-" + [guid]::NewGuid().ToString("N"))
        # TLS 1.2 explicito: PowerShell 5.1 aun negocia TLS 1.0 por defecto.
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $CmdlineToolsUrl -OutFile $zip -UseBasicParsing
        New-Item -ItemType Directory -Path $temporal -Force | Out-Null
        Expand-Archive -Path $zip -DestinationPath $temporal -Force
        # El zip trae "cmdline-tools\..."; sdkmanager exige que cuelgue de "latest".
        $destino = Join-Path $sdkRoot "cmdline-tools\latest"
        New-Item -ItemType Directory -Path $destino -Force | Out-Null
        Copy-Item -Path (Join-Path $temporal "cmdline-tools\*") -Destination $destino -Recurse -Force
        Remove-Item $temporal -Recurse -Force
        Remove-Item $zip -Force
        $sdkManager = Find-SdkManager $sdkRoot
        if ($null -eq $sdkManager) { Fail "No se pudo instalar sdkmanager en $sdkRoot." }
    }
    Write-Ok "sdkmanager en $sdkManager"

    Write-Note "Continuar acepta las licencias del SDK de Android (Google)."
    if (-not (Confirm-Step "Aceptar las licencias e instalar los paquetes del SDK?")) {
        Fail "Sin licencias aceptadas no se puede compilar el APK."
    }
    # sdkmanager pregunta una vez por licencia; se responde que si a todas.
    $sies = (1..40 | ForEach-Object { "y" }) -join "`n"
    $sies | & $sdkManager "--sdk_root=$sdkRoot" --licenses | Out-Null
    Invoke-Tool -Exe $sdkManager -Arguments @("--sdk_root=$sdkRoot", "platform-tools", $AndroidPlatform, $AndroidBuildTools)

    $env:ANDROID_HOME = $sdkRoot
    $env:ANDROID_SDK_ROOT = $sdkRoot
    [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkRoot, "User")

    # Gradle lee la ruta del SDK de local.properties, que no va a git.
    $localProps = Join-Path $Root "local.properties"
    if (-not (Test-Path $localProps)) {
        $escapada = $sdkRoot.Replace("\", "\\")
        Set-Content -Path $localProps -Value "sdk.dir=$escapada" -Encoding UTF8
        Write-Ok "local.properties creado"
    }
    Write-Ok "SDK listo en $sdkRoot"
}

function Initialize-WorkerDeps {
    Write-Step "Dependencias del Worker"
    if (-not (Test-Path (Join-Path $WorkerDir "node_modules"))) {
        Invoke-Tool -Exe "npm.cmd" -Arguments @("install") -WorkDir $WorkerDir
    }
    Write-Ok "node_modules listo"
}

function Initialize-Cloudflare {
    Write-Step "Sesion de Cloudflare"
    Push-Location $WorkerDir
    try {
        & npx.cmd wrangler whoami | Out-Null
        $sesion = ($LASTEXITCODE -eq 0)
    } finally {
        Pop-Location
    }
    if ($sesion) {
        Write-Ok "Ya hay sesion de wrangler"
        return
    }
    Write-Note "No hay sesion. Se abrira el navegador para autorizar wrangler."
    if (-not (Confirm-Step "Iniciar sesion en Cloudflare ahora?")) {
        Fail "Sin sesion de Cloudflare no se puede desplegar."
    }
    Invoke-Tool -Exe "npx.cmd" -Arguments @("wrangler", "login") -WorkDir $WorkerDir
    Write-Ok "Sesion iniciada"
}

function Test-Configuracion {
    Write-Step "Configuracion del proyecto"
    $toml = Get-Content (Join-Path $WorkerDir "wrangler.toml") -Raw
    if ($toml -match "REEMPLAZAR_CON_EL_ID") {
        Write-Note "wrangler.toml todavia tiene el database_id de ejemplo."
        Write-Note "Lanza el script con -InitBackend, o pega a mano el id que da 'wrangler d1 create vivace'."
    } else {
        Write-Ok "wrangler.toml con database_id"
    }
    if (Test-Path (Join-Path $Root "keystore.properties")) {
        Write-Ok "keystore.properties presente (el APK saldra firmado de release)"
    } else {
        Write-Note "Falta keystore.properties: assembleRelease firmaria con la clave de depuracion."
        Write-Note "Crea la clave UNA sola vez y guardala; si cambia, nadie podra actualizar la app:"
        Write-Note "  keytool -genkeypair -v -keystore vivace-release.jks -alias vivace -keyalg RSA -keysize 2048 -validity 10000"
    }
}

# ------------------------------------------------------------------- backend --

function Get-BucketName {
    $toml = Get-Content (Join-Path $WorkerDir "wrangler.toml") -Raw
    if ($toml -match 'bucket_name\s*=\s*"([^"]+)"') { return $Matches[1] }
    Fail "No se encontro bucket_name en wrangler.toml."
}

# Las tablas nuevas se anaden con el mismo fichero que crea las de cero: todo
# el esquema es CREATE ... IF NOT EXISTS.
function Invoke-ApplySchema {
    Write-Step "Aplicar el esquema a la base de produccion"
    Write-Note "Solo crea lo que falte (tablas e indices). No borra ni reescribe datos."
    if (-not (Confirm-Step "Aplicar schema.sql sobre la base 'vivace'?")) {
        Write-Note "Cancelado."
        return
    }
    Invoke-Tool -Exe "npx.cmd" -WorkDir $WorkerDir -Arguments @(
        "wrangler", "d1", "execute", "vivace", "--remote", "--file=schema.sql")

    # migrations.sql lleva los ALTER TABLE. SQLite no tiene "ADD COLUMN IF NOT
    # EXISTS", asi que al repetirlo falla con "duplicate column name": eso es
    # justo lo que se espera cuando ya estaba aplicado, y no es un error.
    Write-Info "Cambios sobre tablas existentes (migrations.sql)..."
    Push-Location $WorkerDir
    try {
        & npx.cmd wrangler d1 execute vivace --remote --file=migrations.sql
        $codigo = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($codigo -ne 0) {
        Write-Note "Alguna migracion no se aplico. Si el motivo es 'duplicate column name',"
        Write-Note "ya estaba puesta y no hay nada que hacer; cualquier otro motivo, miralo."
    }
    Write-Ok "Esquema al dia"
}

function Invoke-InitBackend {
    Write-Step "Puesta en marcha del backend (solo la primera vez)"
    $bucket = Get-BucketName
    if (-not (Confirm-Step "Crear bucket '$bucket', base D1 'vivace' y secretos en tu cuenta de Cloudflare?")) {
        Write-Note "Puesta en marcha cancelada."
        return
    }
    # Si ya existen, wrangler devuelve error: se avisa pero no se aborta.
    Push-Location $WorkerDir
    try {
        & npx.cmd wrangler r2 bucket create $bucket
        if ($LASTEXITCODE -ne 0) { Write-Note "El bucket ya existia o no se pudo crear; sigo." }
        & npx.cmd wrangler d1 create vivace
        if ($LASTEXITCODE -ne 0) { Write-Note "La base ya existia o no se pudo crear; sigo." }
    } finally {
        Pop-Location
    }
    Write-Note "Copia el database_id que ha impreso wrangler en worker/wrangler.toml antes de seguir."
    if (-not (Confirm-Step "Ya esta el database_id en wrangler.toml?")) {
        Fail "Pega el database_id y vuelve a lanzar -InitBackend."
    }
    Invoke-Tool -Exe "npx.cmd" -WorkDir $WorkerDir -Arguments @(
        "wrangler", "d1", "execute", "vivace", "--remote", "--file=schema.sql")

    Write-Note "Ahora los dos secretos. No se guardan en el repositorio ni se muestran por pantalla."
    Write-Note "AUTH_SECRET firma las sesiones (JWT): una cadena larga y aleatoria."
    Invoke-Tool -Exe "npx.cmd" -WorkDir $WorkerDir -Arguments @("wrangler", "secret", "put", "AUTH_SECRET")
    Write-Note "SYNC_TOKEN es el token heredado que usan /admin y las rutas antiguas."
    Invoke-Tool -Exe "npx.cmd" -WorkDir $WorkerDir -Arguments @("wrangler", "secret", "put", "SYNC_TOKEN")
    Write-Ok "Backend preparado. El primer usuario que se registre sera el administrador."
}

# ---------------------------------------------------------------- despliegue --

function Publish-Worker {
    Write-Step "Despliegue del Worker"
    $toml = Get-Content (Join-Path $WorkerDir "wrangler.toml") -Raw
    if ($toml -match "REEMPLAZAR_CON_EL_ID") {
        Fail "wrangler.toml sigue con el database_id de ejemplo. Lanza -InitBackend primero."
    }
    Invoke-Tool -Exe "npm.cmd" -Arguments @("run", "check") -WorkDir $WorkerDir
    if (-not $SkipTests) {
        Invoke-Tool -Exe "npm.cmd" -Arguments @("test") -WorkDir $WorkerDir
    }
    if (-not (Confirm-Step "Publicar el Worker (web, API y panel) en Cloudflare?")) {
        Write-Note "Despliegue del Worker cancelado."
        return
    }
    $salida = Invoke-Tool -Exe "npx.cmd" -Arguments @("wrangler", "deploy") -WorkDir $WorkerDir -Capture
    $salida | ForEach-Object { Write-Host $_ }
    # De la salida sale la URL publica; sirve para comparar versiones despues.
    if ([string]::IsNullOrWhiteSpace($script:BaseUrlResuelta)) {
        $encontrada = $salida | Select-String -Pattern 'https://\S+workers\.dev\S*' | Select-Object -First 1
        if ($null -ne $encontrada) {
            $script:BaseUrlResuelta = $encontrada.Matches[0].Value.TrimEnd("/")
            Write-Info "URL detectada: $script:BaseUrlResuelta"
        }
    }
    Write-Ok "Worker publicado"
}

function Get-AppVersion {
    $gradle = Get-Content (Join-Path $Root "app\build.gradle.kts") -Raw
    if ($gradle -notmatch 'versionCode\s*=\s*(\d+)') { Fail "No se pudo leer versionCode de app/build.gradle.kts." }
    $codigo = [int]$Matches[1]
    if ($gradle -notmatch 'versionName\s*=\s*"([^"]+)"') { Fail "No se pudo leer versionName de app/build.gradle.kts." }
    return [pscustomobject]@{ Code = $codigo; Name = $Matches[1] }
}

function Test-VersionPublicada {
    param([int]$CodigoNuevo)
    if ([string]::IsNullOrWhiteSpace($script:BaseUrlResuelta)) {
        Write-Note "Sin -BaseUrl no se comprueba contra la version ya publicada."
        return
    }
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $actual = Invoke-RestMethod -Uri ($script:BaseUrlResuelta + "/update") -UseBasicParsing -TimeoutSec 20
    } catch {
        Write-Note "No se pudo consultar la version publicada: $($_.Exception.Message)"
        return
    }
    if ($null -eq $actual -or -not ($actual.PSObject.Properties.Name -contains "versionCode")) {
        Write-Info "Todavia no hay ninguna version publicada."
        return
    }
    $publicado = [int]$actual.versionCode
    Write-Info "Publicado ahora mismo: versionCode $publicado"
    if ($CodigoNuevo -le $publicado -and -not $Force) {
        Fail ("El versionCode a publicar ({0}) no es mayor que el publicado ({1}). Subelo en app/build.gradle.kts." -f $CodigoNuevo, $publicado)
    }
}

function Publish-App {
    Write-Step "APK de release"
    $keystore = Join-Path $Root "keystore.properties"
    if (-not (Test-Path $keystore) -and -not $AllowDebugSigning) {
        Fail ("Falta keystore.properties: el APK saldria firmado con la clave de depuracion y " +
              "nadie podria actualizar por encima. Crealo (ver README) o pasa -AllowDebugSigning si es una prueba.")
    }
    if (-not (Test-Path $keystore)) {
        Write-Note "Firmando con la clave de depuracion (-AllowDebugSigning). No distribuyas este APK."
    }

    $version = Get-AppVersion
    Write-Info ("versionCode {0}, versionName {1}" -f $version.Code, $version.Name)
    Test-VersionPublicada -CodigoNuevo $version.Code

    $gradlew = Join-Path $Root "gradlew.bat"
    if (-not $SkipTests) {
        Invoke-Tool -Exe $gradlew -Arguments @("testDebugUnitTest", "--console=plain")
    }
    Invoke-Tool -Exe $gradlew -Arguments @(":app:assembleRelease", "--console=plain")

    $apk = Join-Path $Root "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path $apk)) { Fail "No se genero $apk." }
    Write-Ok ("APK listo: {0} ({1} MB)" -f $apk, [math]::Round((Get-Item $apk).Length / 1MB, 1))

    # latest.json es lo que consulta la app al arrancar para ofrecer la actualizacion.
    $latest = [ordered]@{
        versionCode = $version.Code
        versionName = $version.Name
        notes       = $Notes
        apkUrl      = "/update/apk"
    }
    $latestPath = Join-Path $env:TEMP "vivace-latest.json"
    ($latest | ConvertTo-Json -Compress) | Set-Content -Path $latestPath -Encoding UTF8
    Write-Info ("latest.json: " + (Get-Content $latestPath -Raw).Trim())

    $bucket = Get-BucketName
    Write-Note "Subir el APK lo pone a disposicion de todo el que tenga la app instalada."
    if (-not (Confirm-Step ("Publicar la version {0} ({1}) en el bucket '{2}'?" -f $version.Name, $version.Code, $bucket))) {
        Write-Note "Publicacion cancelada. El APK sigue en $apk"
        return
    }
    Invoke-Tool -Exe "npx.cmd" -WorkDir $WorkerDir -Arguments @(
        "wrangler", "r2", "object", "put", "$bucket/app/app-release.apk",
        "--file=$apk", "--content-type=application/vnd.android.package-archive", "--remote")
    Invoke-Tool -Exe "npx.cmd" -WorkDir $WorkerDir -Arguments @(
        "wrangler", "r2", "object", "put", "$bucket/app/latest.json",
        "--file=$latestPath", "--content-type=application/json", "--remote")
    Remove-Item $latestPath -Force
    Write-Ok ("Version {0} ({1}) publicada" -f $version.Name, $version.Code)
}

# ------------------------------------------------------------------ cuentas --

# Consulta D1 por wrangler. Va con tu acceso de Cloudflare, no con una sesion
# de la aplicacion: sirve aunque nadie tenga la contrasena del administrador.
function Invoke-D1 {
    param([string]$Sql)
    Invoke-Tool -Exe "npx.cmd" -WorkDir $WorkerDir -Arguments @(
        "wrangler", "d1", "execute", "vivace", "--remote", "--command", $Sql)
}

# En SQL una comilla simple se escapa duplicandola.
function ConvertTo-SqlLiteral {
    param([string]$Texto)
    return "'" + $Texto.Replace("'", "''") + "'"
}

function Show-Users {
    Write-Step "Usuarios en la base"
    Invoke-D1 ("SELECT email, role, datetime(created_at / 1000, 'unixepoch') AS alta " +
               "FROM users ORDER BY created_at ASC")
    Write-Info "El catalogo publico de la web es el del administrador MAS ANTIGUO de esta lista."
}

function Invoke-PromoteAdmin {
    Write-Step "Cambiar el rol de una cuenta"
    if ([string]::IsNullOrWhiteSpace($AdminEmail)) {
        Fail "Falta -AdminEmail con la cuenta cuyo rol quieres cambiar."
    }
    Show-Users

    $correo = ConvertTo-SqlLiteral $AdminEmail.Trim().ToLowerInvariant()
    if ($Role -eq "admin") {
        Write-Note "Vas a dar permisos de administrador sobre TODO el contenido a $AdminEmail."
        Write-Note "El admin ve, edita y borra partituras de cualquiera, y reparte roles."
    } elseif ($Role -eq "editor") {
        Write-Note "El editor gestiona el catalogo: edita y despublica partituras publicas,"
        Write-Note "resuelve propuestas y mantiene el diccionario de acordes."
    } else {
        Write-Note "Vas a QUITARLE los permisos a $AdminEmail y dejarlo como usuario normal."
    }
    if (-not (Confirm-Step "Poner a $AdminEmail como '$Role'?")) {
        Write-Note "Cancelado. No se ha tocado la base."
        return
    }
    Invoke-D1 ("UPDATE users SET role = '$Role' WHERE email_lower = $correo")
    Show-Users
    Write-Note "Si arriba sigue habiendo un admin mas antiguo, el catalogo seguira siendo el suyo."
    Write-Note "Para que el tuyo mande, degrada al viejo con:"
    Write-Note ("  npx wrangler d1 execute vivace --remote --command " +
                "`"UPDATE users SET role='user' WHERE email_lower='el-viejo@ejemplo.com'`"")
}

# -------------------------------------------------------------- catalogo --

# Pide la URL publica si no vino por parametro ni salio de wrangler deploy.
function Get-BaseUrl {
    if (-not [string]::IsNullOrWhiteSpace($script:BaseUrlResuelta)) { return $script:BaseUrlResuelta }
    $entrada = Read-Host "URL publica del Worker (https://...)"
    if ([string]::IsNullOrWhiteSpace($entrada)) { Fail "Hace falta la URL del Worker." }
    $script:BaseUrlResuelta = $entrada.Trim().TrimEnd("/")
    return $script:BaseUrlResuelta
}

# POST con cuerpo JSON codificado en UTF-8 a mano: PowerShell 5.1 manda el
# cuerpo en la codepage del sistema y destroza las tildes.
function Invoke-JsonPost {
    param([string]$Uri, [hashtable]$Body, [string]$Token)
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $cabeceras = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) { $cabeceras["Authorization"] = "Bearer $Token" }
    $parametros = @{
        Uri         = $Uri
        Method      = "Post"
        Headers     = $cabeceras
        ContentType = "application/json; charset=utf-8"
        TimeoutSec  = 60
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $parametros["Body"] = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress))
    }
    return Invoke-RestMethod @parametros
}

# Invoke-RestMethod tira la respuesta cuando el codigo es de error, asi que el
# mensaje del servidor ("sin base de datos", el motivo del 500...) se pierde.
# Esto lo rescata del stream para poder ensenarlo.
function Get-ErrorBody {
    param($Registro)
    try {
        $respuesta = $Registro.Exception.Response
        if ($null -eq $respuesta) { return "" }
        $lector = New-Object IO.StreamReader($respuesta.GetResponseStream())
        try { return $lector.ReadToEnd().Trim() } finally { $lector.Dispose() }
    } catch {
        return ""
    }
}

function Format-HttpError {
    param($Registro, [string]$Prefijo)
    $cuerpo = Get-ErrorBody $Registro
    $texto = "{0}: {1}" -f $Prefijo, $Registro.Exception.Message
    if (-not [string]::IsNullOrWhiteSpace($cuerpo)) {
        $texto += "`n      respuesta: $cuerpo"
    }
    # Un 404 en una ruta de la API casi siempre significa que el Worker
    # desplegado es anterior al codigo local, no que la ruta no exista.
    $codigo = 0
    try { $codigo = [int]$Registro.Exception.Response.StatusCode } catch { $codigo = 0 }
    if ($codigo -eq 404) {
        $texto += "`n      El Worker desplegado no conoce esa ruta. Publica primero el codigo:"
        $texto += "`n        .\tools\deploy.ps1 -Worker"
    }
    return $texto
}

# Clasificar es adivinar: por eso va en dos pasos, primero en seco.
function Invoke-Categorize {
    Write-Step "Categorias del catalogo"
    $base = Get-BaseUrl
    $sesion = Get-EditorSession $base

    Write-Info "Reglas en worker/src/genres.js: artista primero, luego el titulo,"
    Write-Info "y 'Varios' para lo que no encaje. Se corrigen a mano cuando quieras."
    Write-Note "Esto usa una ruta nueva de la API: si no has desplegado el Worker"
    Write-Note "despues del ultimo cambio, hazlo antes (.\tools\deploy.ps1 -Worker)."

    $resumen = Invoke-Categorize-Pasada $base $sesion.token $true
    if ($resumen.Total -eq 0) {
        Write-Ok "No hay nada que clasificar."
        return
    }
    Write-Note ("Se asignaria categoria a {0} partituras:" -f $resumen.Total)
    $resumen.Tally.GetEnumerator() | Sort-Object -Property Value -Descending | ForEach-Object {
        Write-Host ("      {0,-18} {1}" -f $_.Key, $_.Value) -ForegroundColor DarkGray
    }
    if ($Overwrite) { Write-Note "-Overwrite: se pisaran tambien las que YA tienen categoria." }
    if (-not (Confirm-Step "Aplicar estas categorias?")) {
        Write-Note "Cancelado. No se ha tocado nada."
        return
    }
    $aplicado = Invoke-Categorize-Pasada $base $sesion.token $false
    Write-Ok ("Categorias asignadas a {0} partituras" -f $aplicado.Total)
}

# Una pasada completa, encadenando tandas con el cursor que devuelve el Worker.
function Invoke-Categorize-Pasada {
    param([string]$Base, [string]$Token, [bool]$EnSeco)
    $tally = @{}
    $total = 0
    $cursor = ""
    $tanda = 0
    do {
        $tanda++
        try {
            $r = Invoke-JsonPost -Uri "$Base/api/genres/auto" -Token $Token -Body @{
                dryRun = $EnSeco; overwrite = [bool]$Overwrite; cursor = $cursor; limit = 200
            }
        } catch {
            Fail (Format-HttpError $_ "Fallo la clasificacion")
        }
        $total += [int]$r.wouldUpdate
        foreach ($p in $r.tally.PSObject.Properties) {
            $tally[$p.Name] = [int]$tally[$p.Name] + [int]$p.Value
        }
        $cursor = $r.cursor
        if (-not $EnSeco -and [int]$r.updated -gt 0) {
            Write-Info ("tanda {0}: {1} clasificadas" -f $tanda, $r.updated)
        }
        if ($tanda -ge 500) { Write-Note "Demasiadas tandas; se para aqui."; break }
    } while (-not $r.done)
    return @{ Total = $total; Tally = $tally }
}

# Pide credenciales y devuelve la sesion. La contrasena la escribe la persona y
# se libera de memoria en cuanto se ha usado.
function Get-EditorSession {
    param([string]$Base, [switch]$SoloAdmin)
    if ($SoloAdmin) {
        Write-Note "Entra con la cuenta de ADMINISTRADOR (el primer usuario que se registro)."
    } else {
        Write-Note "Entra con una cuenta de editor o administrador."
    }
    $email = Read-Host "Email"
    $segura = Read-Host "Contrasena" -AsSecureString
    $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    $sesion = $null
    try {
        $clave = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
        try {
            $sesion = Invoke-JsonPost -Uri "$Base/auth/login" -Body @{ email = $email; password = $clave }
        } catch {
            Fail (Format-HttpError $_ "No se pudo iniciar sesion")
        }
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
        $clave = $null
    }
    if ($null -eq $sesion -or [string]::IsNullOrWhiteSpace($sesion.token)) {
        Fail "El servidor no devolvio ninguna sesion."
    }
    $rol = $sesion.user.role
    if ($SoloAdmin -and $rol -ne "admin") {
        Fail ("La cuenta '{0}' no es administradora." -f $email)
    }
    if (-not $SoloAdmin -and $rol -ne "admin" -and $rol -ne "editor") {
        Fail ("La cuenta '{0}' no es editora ni administradora." -f $email)
    }
    Write-Ok ("Sesion de {0}: {1}" -f $rol, $sesion.user.email)
    return $sesion
}

function Invoke-PublishCatalog {
    Write-Step "Catalogo: indexar en la base lo que ya estaba en R2"
    $base = Get-BaseUrl

    Write-Info "Las partituras anteriores al multiusuario estan en R2 pero no en la base,"
    Write-Info "y la web lista desde la base: por eso el catalogo sale vacio."
    $sesion = Get-EditorSession $base -SoloAdmin

    if ($CatalogVisibility -eq "public") {
        Write-Note "Con visibilidad 'public' esas partituras quedan visibles para CUALQUIERA que entre en la web."
    } else {
        Write-Note "Con visibilidad 'private' solo las veras tu en 'Mis partituras'."
    }
    if (-not (Confirm-Step ("Indexar las partituras de R2 como '{0}' en {1}?" -f $CatalogVisibility, $base))) {
        Write-Note "Migracion cancelada. No se ha tocado nada."
        return
    }

    # El Worker migra por tandas (tiene un tope de subpeticiones por invocacion),
    # asi que aqui se repite con el cursor que devuelve hasta que dice done.
    $totalNuevas = 0
    $totalSaltadas = 0
    $tanda = 0
    $cursor = $null
    $resultado = $null
    do {
        $tanda++
        $uri = "$base/admin/migrate?visibility=$CatalogVisibility"
        if (-not [string]::IsNullOrWhiteSpace($cursor)) {
            $uri = $uri + "&cursor=" + [Uri]::EscapeDataString($cursor)
        }
        try {
            $resultado = Invoke-JsonPost -Uri $uri -Token $sesion.token
        } catch {
            Write-Note (("Se corto en la tanda {0}. Lo indexado hasta aqui se queda: vuelve a lanzar " +
                         "-PublishCatalog y sigue por donde iba.") -f $tanda)
            Fail (Format-HttpError $_ "Fallo la migracion")
        }
        $totalNuevas += [int]$resultado.imported
        $totalSaltadas += [int]$resultado.skipped
        Write-Info ("tanda {0}: {1} nuevas, {2} ya estaban (acumulado: {3})" -f
                    $tanda, $resultado.imported, $resultado.skipped, $totalNuevas)
        $cursor = $resultado.cursor
        # Cinturon: si el servidor devolviera siempre cursor, no se gira eternamente.
        if ($tanda -ge 500) {
            Write-Note "Demasiadas tandas seguidas; se para aqui. Vuelve a lanzarlo para continuar."
            break
        }
    } while (-not $resultado.done)

    Write-Ok ("Indexadas {0} partituras, {1} ya estaban registradas." -f $totalNuevas, $totalSaltadas)
    if ($totalNuevas -eq 0 -and $totalSaltadas -eq 0) {
        Write-Note "R2 no tenia nada bajo 'songs/'. Comprueba que el bucket es el que esperas."
    }
    Write-Info "Recarga la web: el catalogo se sirve desde $base/api/songs/public"
}

# --------------------------------------------------------------------- main --

if ($All) { $Setup = $true; $Worker = $true; $App = $true }
if (-not ($Setup -or $InitBackend -or $Worker -or $App -or $PublishCatalog -or $ListUsers -or
          $PromoteAdmin -or $ApplySchema -or $Categorize)) {
    $Setup = $true
}

$script:BaseUrlResuelta = $BaseUrl.TrimEnd("/")

Write-Host "Vivace - entorno y despliegue" -ForegroundColor White
Write-Host "Repositorio: $Root" -ForegroundColor DarkGray

# -PublishCatalog solo habla por HTTP con el Worker ya desplegado: no necesita
# ni cadena de compilacion ni sesion de wrangler. -ListUsers y -PromoteAdmin si
# usan wrangler (hablan con D1), pero no la cadena de compilacion de Android.
if ($ListUsers -or $PromoteAdmin -or $ApplySchema) {
    Initialize-Node
    Initialize-WorkerDeps
    Initialize-Cloudflare
}
if ($Setup -or $InitBackend -or $Worker -or $App) {
    Initialize-Node
    if ($Setup -or $App) {
        Initialize-Java
    }
    if ($Setup) {
        Initialize-AndroidSdk
    }
    Initialize-WorkerDeps
    Initialize-Cloudflare
    if ($Setup) {
        Test-Configuracion
    }
}
if ($InitBackend) { Invoke-InitBackend }
if ($ApplySchema) { Invoke-ApplySchema }
if ($Worker) { Publish-Worker }
if ($App) { Publish-App }
if ($ListUsers -and -not $PromoteAdmin) { Show-Users }
if ($PromoteAdmin) { Invoke-PromoteAdmin }
if ($PublishCatalog) { Invoke-PublishCatalog }
if ($Categorize) { Invoke-Categorize }

Write-Step "Hecho"
if ($Setup -and -not ($Worker -or $App -or $PublishCatalog)) {
    Write-Host "  Entorno listo. Para publicar:" -ForegroundColor Gray
    Write-Host "    .\tools\deploy.ps1 -Worker" -ForegroundColor Gray
    Write-Host "    .\tools\deploy.ps1 -App -Notes `"Novedades de esta version`"" -ForegroundColor Gray
}
