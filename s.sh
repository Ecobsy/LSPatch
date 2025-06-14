#!/bin/bash
set -e

# 1. Verificar estado y, opcionalmente, crear una rama de respaldo.
echo "Verifica que no tengas cambios pendientes y, si es necesario, crea una rama de respaldo."
git status
# Opcional: git checkout -b integrar-submodulos

# 2. Desinicializa TODOS los submódulos.
echo "Desinicializando todos los submódulos..."
git submodule deinit -f --all

# 3. Recorre el archivo .gitmodules para procesar cada submódulo definido.
echo "Procesando submódulos definidos en .gitmodules..."
git config --file .gitmodules --get-regexp path | while read key path; do
    # Obtiene el nombre del submódulo a partir de la clave: submodule.[nombre].path
    name=$(echo $key | sed 's/^submodule\.//;s/\.path$//')
    url=$(git config --file .gitmodules --get submodule.${name}.url)
    
    echo "Integrando el submódulo '$name' en la ruta: $path"
    echo "URL: $url"
    
    # 4. Elimina el submódulo del índice y borra la referencia interna.
    git rm -rf "$path"
    rm -rf ".git/modules/$path"
    
    # 5. Clona el repositorio del submódulo en un directorio temporal.
    temp_dir="${path}_temp"
    git clone "$url" "$temp_dir"
    
    # 6. Copia el contenido clonado (incluyendo archivos ocultos) a la ruta definitiva.
    mkdir -p "$path"
    cp -R "$temp_dir"/. "$path"
    
    # 7. Elimina la carpeta .git del contenido integrado del submódulo.
    rm -rf "$path/.git"
    
    # 8. Limpia el directorio temporal.
    rm -rf "$temp_dir"
    
    # 9. Agrega la carpeta integrada al índice.
    git add "$path"
done

# 10. Elimina el archivo .gitmodules, ya que no es necesario tras integrar los submódulos.
rm -f .gitmodules

# ------------------------------------------------------
# Limpieza global: eliminar cualquier carpeta .git (excepto la del directorio principal)
# y removerlas del índice (git rm -r --cached) de forma recursiva.
# ------------------------------------------------------
echo "Buscando y eliminando carpetas '.git' residuales (excepto la principal)..."

# Recorre y remueve de git el cache de cada carpeta .git que se encuentre en niveles inferiores.
find . -mindepth 2 -type d -name ".git" | while read gitdir; do
    echo "Ejecutando: git rm -r --cached $gitdir"
    git rm -r --cached "$gitdir" || true
done

# Ahora elimina físicamente dichas carpetas .git.
find . -mindepth 2 -type d -name ".git" -exec rm -rf {} +

echo "Limpieza completada: No quedan carpetas '.git' residuales fuera del directorio principal."

# 11. Realiza el commit final de todos los cambios.
git commit -m "Integración local de todos los submódulos y limpieza de carpetas .git residuales"
