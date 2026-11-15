# Tareas de Graduación UNAL en Windows (las usa .vscode/tasks.json).
#   -Modo reporte   compila, corre pruebas y mediciones y deja resultados\para_el_reporte.zip (por defecto)
#   -Modo compilar  solo compila
#   -Modo pruebas   compila y corre las pruebas
#   -Modo menu      compila y abre el menú interactivo
# Si javac no está en el PATH, busca un JDK 17 o superior en las carpetas de instalación habituales.
param([ValidateSet('reporte', 'compilar', 'pruebas', 'menu')][string]$Modo = 'reporte')

$ErrorActionPreference = 'Continue'
Set-Location (Split-Path -Parent $PSScriptRoot)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
New-Item -ItemType Directory -Force -Path resultados | Out-Null
$jvm = @('-Dstdout.encoding=UTF-8', '-Dsun.stdout.encoding=UTF-8', '-cp', 'out', 'unal.graduacion.Main')

function Paso([string]$texto) {
    Write-Host ''
    Write-Host "== $texto ==" -ForegroundColor Cyan
}

function Terminar([string]$mensaje) {
    Write-Host ''
    Write-Host $mensaje -ForegroundColor Red
    exit 1
}

function Ejecutar([string]$titulo, [string[]]$argumentos, [string]$registro) {
    Paso $titulo
    & java @jvm @argumentos 2>&1 | ForEach-Object { $linea = "$_"; Write-Host $linea; $linea } |
        Out-File -FilePath $registro -Encoding utf8
    if ($LASTEXITCODE -ne 0) { Terminar "Falló: $titulo (código $LASTEXITCODE). El detalle quedó en $registro." }
}

function VersionMayor([string]$javac) {
    $texto = (& $javac -version 2>&1 | ForEach-Object { "$_" }) -join ' '
    if ($texto -match 'javac\s+(\d+)(?:\.(\d+))?') {
        $mayor = [int]$Matches[1]
        if ($mayor -eq 1 -and $Matches[2]) { $mayor = [int]$Matches[2] }
        return $mayor
    }
    return 0
}

Paso 'Buscando un JDK 17 o superior'
$candidatos = New-Object System.Collections.Generic.List[string]
if ($env:JAVA_HOME) { $candidatos.Add((Join-Path $env:JAVA_HOME 'bin\javac.exe')) }
$enPath = Get-Command javac -ErrorAction SilentlyContinue
if ($enPath) { $candidatos.Add($enPath.Source) }
$raices = @(
    "$env:ProgramFiles\Apache NetBeans\jdk",
    "$env:ProgramFiles\Eclipse Adoptium\*",
    "$env:ProgramFiles\Java\*",
    "$env:ProgramFiles\Microsoft\jdk*",
    "$env:ProgramFiles\Zulu\*",
    "$env:ProgramFiles\Amazon Corretto\*",
    "$env:ProgramFiles\BellSoft\*",
    "$env:ProgramFiles\OpenJDK\*",
    "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\*",
    "$env:USERPROFILE\.jdks\*"
)
foreach ($raiz in $raices) {
    Get-Item -Path (Join-Path $raiz 'bin\javac.exe') -ErrorAction SilentlyContinue |
        ForEach-Object { $candidatos.Add($_.FullName) }
}
$javacElegido = $null
$versionElegida = 0
foreach ($candidato in ($candidatos | Select-Object -Unique)) {
    if (-not (Test-Path $candidato)) { continue }
    $version = VersionMayor $candidato
    Write-Host "  encontrado: $candidato (Java $version)"
    if ($version -gt $versionElegida) { $javacElegido = $candidato; $versionElegida = $version }
}
if (-not $javacElegido -or $versionElegida -lt 17) {
    Terminar 'No se encontró un JDK 17 o superior. Instale uno (por ejemplo, Eclipse Temurin 21 desde https://adoptium.net), cierre VS Code y vuelva a abrirlo.'
}
$binJdk = Split-Path -Parent $javacElegido
$env:JAVA_HOME = Split-Path -Parent $binJdk
$env:PATH = "$binJdk;$env:PATH"
Write-Host "Se usará el JDK de $env:JAVA_HOME (Java $versionElegida)." -ForegroundColor Green
$versionJava = ((& java -version 2>&1 | ForEach-Object { "$_" }) -join [Environment]::NewLine).Trim()
$versionJavac = ((& javac -version 2>&1 | ForEach-Object { "$_" }) -join ' ').Trim()
$heap = & java '-XX:+PrintFlagsFinal' '-version' 2>$null | Select-String -Pattern '\bMaxHeapSize\b' | Select-Object -First 1
$heapGB = ''
if ($heap -and ("$heap" -match '=\s*(\d+)')) { $heapGB = [math]::Round([double]$Matches[1] / 1GB, 1) }

Paso 'Compilando'
if (Test-Path out) { Remove-Item -Recurse -Force out }
$fuentes = (Get-ChildItem -Recurse -Path src -Filter *.java).FullName
& javac -encoding UTF-8 -d out $fuentes
if ($LASTEXITCODE -ne 0) { Terminar 'La compilación falló; revise los errores de arriba.' }
Write-Host "Compilados $($fuentes.Count) archivos fuente."

if ($Modo -eq 'compilar') { exit 0 }
if ($Modo -eq 'pruebas') {
    & java @jvm --pruebas
    exit $LASTEXITCODE
}
if ($Modo -eq 'menu') {
    & java @jvm
    exit $LASTEXITCODE
}

Paso 'Datos del equipo'
$cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
$so = Get-CimInstance Win32_OperatingSystem
$cs = Get-CimInstance Win32_ComputerSystem
$ramGB = [math]::Round($cs.TotalPhysicalMemory / 1GB, 1)
$vscode = ''
if (Get-Command code -ErrorAction SilentlyContinue) { $vscode = (cmd /c 'code --version 2>nul' | Select-Object -First 1) }
$datos = @(
    "procesador: $($cpu.Name.Trim())",
    "nucleos: $($cpu.NumberOfCores)",
    "hilos: $($cpu.NumberOfLogicalProcessors)",
    "frecuencia_maxima_mhz: $($cpu.MaxClockSpeed)",
    "ram_gb: $ramGB",
    "sistema_operativo: $($so.Caption) $($so.Version) (compilacion $($so.BuildNumber), $($so.OSArchitecture))",
    "vscode: $vscode",
    "jvm_memoria_maxima_gb: $heapGB",
    "javac: $versionJavac",
    'java:',
    $versionJava
)
$datos | Out-File -FilePath resultados\equipo.txt -Encoding utf8
$datos | ForEach-Object { Write-Host $_ }

Ejecutar 'Pruebas funcionales' @('--pruebas') 'resultados\pruebas.log'
Ejecutar 'Mediciones de 10^4 a 10^6 asignaturas (tarda unos minutos; no use el computador mientras tanto)' @('--medir') 'resultados\medir.log'
Ejecutar 'Comparación del AVL con las alternativas O(n^2)' @('--medir', '--tamanos', '1000,2000,5000,10000,20000', '--csv', 'resultados/cuadraticas.csv') 'resultados\cuadraticas.log'
Ejecutar 'Calidad de la cota' @('--calidad') 'resultados\calidad.log'

Paso 'Empaquetando'
$archivos = @('resultados\equipo.txt', 'resultados\pruebas.log', 'resultados\medir.log',
              'resultados\tiempos.csv', 'resultados\tiempos_resumen.csv',
              'resultados\cuadraticas.csv', 'resultados\cuadraticas_resumen.csv', 'resultados\calidad.csv')
Compress-Archive -Path $archivos -DestinationPath resultados\para_el_reporte.zip -Force
$zip = (Resolve-Path resultados\para_el_reporte.zip).Path
Write-Host ''
Write-Host 'Listo. Adjunte este archivo en el chat con Claude:' -ForegroundColor Green
Write-Host $zip -ForegroundColor Green
Start-Process explorer.exe -ArgumentList "/select,`"$zip`""
