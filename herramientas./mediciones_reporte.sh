#!/usr/bin/env bash
# Prepara los datos que faltan en el reporte técnico de la Entrega 2 (Linux y macOS).
# En VS Code: Terminal > Ejecutar tarea > "Graduación UNAL: mediciones para el reporte".
# Deja todo en resultados/ y empaqueta lo necesario en resultados/para_el_reporte.zip.
set -u
cd "$(dirname "$0")/.."
mkdir -p resultados
JVM=(-Dstdout.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -cp out unal.graduacion.Main)

paso() { printf '\n== %s ==\n' "$1"; }
terminar() { printf '\n%s\n' "$1" >&2; exit 1; }
ejecutar() {
    local titulo="$1" registro="$2"; shift 2
    paso "$titulo"
    java "${JVM[@]}" "$@" 2>&1 | tee "$registro"
    local codigo=${PIPESTATUS[0]}
    [ "$codigo" -eq 0 ] || terminar "Falló: $titulo (código $codigo). El detalle quedó en $registro."
}

paso 'Revisando Java'
command -v javac >/dev/null || terminar 'No se encontró javac. Instale un JDK 17 o superior.'
version_java=$(java -version 2>&1)
version_javac=$(javac -version 2>&1)
echo "$version_java"; echo "$version_javac"
heap=$(java -XX:+PrintFlagsFinal -version 2>/dev/null | awk '$2 == "MaxHeapSize" {printf "%.1f", $4 / 1073741824}')

paso 'Compilando'
rm -rf out
javac -encoding UTF-8 -d out $(find src -name '*.java') || terminar 'La compilación falló; revise los errores de arriba.'

paso 'Datos del equipo'
if [ "$(uname)" = Darwin ]; then
    cpu=$(sysctl -n machdep.cpu.brand_string); nucleos=$(sysctl -n hw.physicalcpu); hilos=$(sysctl -n hw.logicalcpu)
    ram=$(sysctl -n hw.memsize | awk '{printf "%.1f", $1 / 1073741824}')
    so="macOS $(sw_vers -productVersion) ($(uname -m))"
else
    cpu=$(grep -m1 'model name' /proc/cpuinfo | cut -d: -f2- | sed 's/^ *//')
    hilos=$(grep -c '^processor' /proc/cpuinfo)
    nucleos=$(lscpu 2>/dev/null | awk -F: '/^Core\(s\) per socket/ {c=$2} /^Socket\(s\)/ {s=$2} END {if (c && s) print c * s}')
    ram=$(awk '/MemTotal/ {printf "%.1f", $2 / 1048576}' /proc/meminfo)
    so="$( (. /etc/os-release 2>/dev/null && echo "$PRETTY_NAME") || uname -s) $(uname -r) ($(uname -m))"
fi
vscode=$(command -v code >/dev/null && code --version 2>/dev/null | head -1)
{
    echo "procesador: $cpu"
    echo "nucleos: ${nucleos:-}"
    echo "hilos: $hilos"
    echo "frecuencia_maxima_mhz: "
    echo "ram_gb: $ram"
    echo "sistema_operativo: $so"
    echo "vscode: ${vscode:-}"
    echo "jvm_memoria_maxima_gb: ${heap:-}"
    echo "javac: $version_javac"
    echo "java:"
    echo "$version_java"
} | tee resultados/equipo.txt

ejecutar 'Pruebas funcionales' resultados/pruebas.log --pruebas
ejecutar 'Mediciones de 10^4 a 10^6 asignaturas (tarda unos minutos; no use el computador mientras tanto)' resultados/medir.log --medir
ejecutar 'Comparación del AVL con las alternativas O(n^2)' resultados/cuadraticas.log --medir --tamanos 1000,2000,5000,10000,20000 --csv resultados/cuadraticas.csv
ejecutar 'Calidad de la cota' resultados/calidad.log --calidad

paso 'Empaquetando'
rm -f resultados/para_el_reporte.zip
if command -v zip >/dev/null; then
    (cd resultados && zip -q para_el_reporte.zip equipo.txt pruebas.log medir.log tiempos.csv tiempos_resumen.csv cuadraticas.csv cuadraticas_resumen.csv calidad.csv)
else
    python3 -c 'import zipfile,sys; z=zipfile.ZipFile("resultados/para_el_reporte.zip","w",zipfile.ZIP_DEFLATED); [z.write("resultados/"+a, a) for a in sys.argv[1:]]' equipo.txt pruebas.log medir.log tiempos.csv tiempos_resumen.csv cuadraticas.csv cuadraticas_resumen.csv calidad.csv
fi
printf '\nListo. Adjunte este archivo en el chat con Claude:\n%s\n' "$(pwd)/resultados/para_el_reporte.zip"
