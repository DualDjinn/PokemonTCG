# Guía de trabajo con Git — TPI Pokémon TCG

Esta guía está pensada para que todos podamos trabajar sobre el mismo proyecto sin pisarnos los cambios. Seguí los pasos en orden cada vez que te sentés a trabajar.

---

## ¿Qué es una rama y por qué la usamos?

En Git, una **rama** es una copia aislada del proyecto donde podés hacer cambios sin afectar el trabajo de los demás. Cada integrante trabaja en su propia rama y, cuando termina una funcionalidad, la integra al proyecto principal (la rama `main`) a través de un **Pull Request** que el equipo revisa.

La rama `main` siempre tiene que tener código que funcione. Nunca se trabaja directamente sobre ella.

---

## Configuración inicial (solo la primera vez)

Antes de hacer cualquier cosa con Git, configurá tu nombre y email. Esto es lo que va a aparecer en cada commit que hagas.

```bash
git config --global user.name "Tu Nombre Completo"
git config --global user.email "tu-email@ejemplo.com"
```

---

## Paso 1 — Clonar el repositorio (solo si no lo tenés)

Si es la primera vez que vas a trabajar en el proyecto, descargalo con:

```bash
git clone <url-del-repositorio>
```

La URL la encontrás en el repositorio de GitHub, en el botón verde que dice **Code**.

Una vez clonado, entrá a la carpeta del proyecto:

```bash
cd tpi-pokemon-2w2-07
```

---

## Paso 2 — Crear tu rama (solo la primera vez que trabajás)

Cada integrante tiene su propia rama. El nombre debe seguir el formato `legajo-nombre-completo`, todo en minúsculas y con guiones en lugar de espacios.

```bash
git checkout -b 123456-juan-perez
```

Esto crea la rama y te mueve a ella automáticamente. **Solo hacés esto una vez**. Las veces siguientes, tu rama ya existe y pasás directo al Paso 3.

---

## Paso 3 — Actualizar main antes de empezar a trabajar

Antes de tocar cualquier archivo, traete los últimos cambios que hicieron tus compañeros en `main`. Esto evita que trabajes sobre código desactualizado.

```bash
git checkout main
git pull origin main
git checkout 123456-juan-perez
```

Ahora tu rama local sabe que `main` se actualizó, pero todavía no tiene esos cambios. El siguiente paso los incorpora.

---

## Paso 4 — Incorporar los cambios de main a tu rama (merge)

Con este comando traés todo lo nuevo de `main` a tu rama:

```bash
git merge main
```

Si Git no encuentra ningún conflicto, el merge se completa solo y podés pasar al Paso 6.

Si hay conflictos, Git te avisa y tenés que resolverlos (ver Paso 5).

---

## Paso 5 — Resolver conflictos

Un conflicto ocurre cuando vos y otro compañero modificaron el mismo fragmento del mismo archivo. Git no sabe cuál de las dos versiones conservar y te lo marca así dentro del archivo:

```
tu versión del código


Para resolverlo:

1. Abrí el archivo en conflicto (VS Code los resalta automáticamente).
2. Decidí qué versión conservar, o combiná ambas manualmente. Borrá las líneas con `<<<<<<<`, `=======` y `>>>>>>>`.
3. Guardá el archivo.
4. Una vez resueltos todos los conflictos, marcalos como resueltos:

```bash
git add nombre-del-archivo-en-conflicto
```

5. Finalizá el merge:

```bash
git commit
```

---

## Paso 6 — Trabajar en tu rama

Ahora sí, hacé los cambios que corresponden a tu tarea. Cuando termines una parte con sentido (no hace falta que sea todo de una vez), guardá tu progreso con un commit.

Primero revisá qué archivos modificaste:

```bash
git status
```

Agregá los archivos que quieras incluir en el commit:

```bash
git add nombre-del-archivo
```

Si querés agregar todos los archivos modificados de una vez:

```bash
git add .
```

Luego creá el commit con un mensaje claro que describa qué hiciste:

```bash
git commit -m "Agrega validación de mazo en DeckService"
```

> Escribí mensajes en español, en presente, y que expliquen **qué** hace el cambio. Evitá mensajes vagos como `"cambios"`, `"fix"` o `"wip"`.

Podés hacer tantos commits como quieras mientras trabajás. No hace falta que cada commit sea perfecto.

---

## Paso 7 — Actualizar main y mergear antes de subir tus cambios

Antes de subir tu rama, volvé a traer los últimos cambios de `main`. Es probable que tus compañeros hayan mergeado cosas mientras vos trabajabas.

```bash
git checkout main
git pull origin main
git checkout 123456-juan-perez
git merge main
```

Si aparecen conflictos, resolverlos como en el Paso 5.

---

## Paso 8 — Subir tu rama a GitHub

Una vez que tu rama tiene todo en orden, subila:

```bash
git push origin 123456-juan-perez
```

Si es la primera vez que subís la rama, puede que Git te pida usar `--set-upstream`. En ese caso ejecutá exactamente lo que te sugiere, que es algo como:

```bash
git push --set-upstream origin 123456-juan-perez
```

Las veces siguientes alcanza con `git push`.

---

## Paso 9 — Crear un Pull Request en GitHub

1. Entrá al repositorio en GitHub.
2. GitHub generalmente muestra un banner amarillo que dice **"Compare & pull request"** cuando detecta que subiste una rama recientemente. Hacé clic ahí.
3. Si no aparece el banner, andá a la pestaña **Pull requests** → **New pull request**.
4. Asegurate de que la base sea `main` y que el compare sea tu rama.
5. Escribí un título descriptivo y en la descripción explicá brevemente qué cambiaste y por qué.
6. Asigná a tus compañeros como revisores si el proyecto lo requiere.
7. Hacé clic en **Create pull request**.

Un compañero o el docente va a revisar los cambios antes de que se integren a `main`.

---

## Resumen rápido para el día a día

```bash
# Al empezar a trabajar
git checkout main
git pull origin main
git checkout mi-rama
git merge main

# Mientras trabajo
git add .
git commit -m "Descripción de lo que hice"

# Antes de pushear
git checkout main
git pull origin main
git checkout mi-rama
git merge main
# (resolver conflictos si los hay)
git push origin mi-rama
```

---

## Errores frecuentes

| Situación | Qué hacer |
|---|---|
| `error: Your local changes would be overwritten by merge` | Hacé commit o `git stash` de tus cambios antes de hacer el merge. |
| `rejected — non-fast-forward` al hacer push | Alguien subió cambios antes que vos. Hacé pull, mergea y volvé a pushear. |
| Me fui sin querer a `main` y empecé a editar | Usá `git stash`, volvé a tu rama con `git checkout mi-rama` y aplicá los cambios con `git stash pop`. |
| Quiero deshacer el último commit (que no subí) | `git reset --soft HEAD~1` (conserva los cambios en los archivos). |
