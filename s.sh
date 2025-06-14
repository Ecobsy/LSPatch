#!/bin/bash
set -e

# 1. Verifica que no tengas cambios pendientes y crea una rama de respaldo.
echo "Verifica que no tengas cambios pendientes y, si es necesario, crea una rama de respaldo."
git status
# (Opcional) git checkout -b integrar-submodulos

# 2. Desinicializa TODOS los submódulos.
echo "Desinicializando todos los submódulos..."
git submodule deinit -f --all

# 3. Recorre el archivo .gitmodules para obtener cada submódulo.
echo "Procesando submódulos definidos en .gitmodules..."
git config --file .gitmodules --get-regexp path | while read key path; do
    # Extrae el nombre del submódulo a partir de la clave: submodule.[nombre].path
    name=$(echo $key | sed 's/^submodule\.//;s/\.path$//')
    url=$(git config --file .gitmodules --get submodule.${name}.url)
    
    echo "Integrando el submódulo '$name' en la ruta: $path"
    echo "URL: $url"
    
    # 4. Elimina el submódulo del índice y las referencias internas.
    git rm -rf "$path"
    rm -rf ".git/modules/$path"
    
    # 5. Clona el repositorio del submódulo en un directorio temporal.
    temp_dir="${path}_temp"
    git clone "$url" "$temp_dir"
    
    # 6. Copia el contenido a la ubicación definitiva (incluyendo archivos ocultos).
    mkdir -p "$path"
    cp -R "$temp_dir"/. "$path"
    
    # Elimina la carpeta .git para que la integración quede "desado" de historial propio.
    rm -rf "$path/.git"
    
    # 7. Limpia el directorio temporal.
    rm -rf "$temp_dir"
    
    # Añade la carpeta integrada al índice de Git.
    git add "$path"
done

# 8. Elimina el archivo .gitmodules ya que ya no es necesario.
rm -f .gitmodules

echo "Todos los submódulos han sido integrados localmente."

# 9. Finalmente, confirma los cambios.
git commit -m "Integración local de todos los submódulos de forma recursiva"
