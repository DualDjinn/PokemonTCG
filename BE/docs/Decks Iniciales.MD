
TAREAS: 
- Agregar un nombre de entrenador al Player, es decir, que tenga su nombre de usuario y a demas nombre de entrenador el cual seria su nombre visual que apareceria en el juego en si.
- Crear los controllers para el Player + los servicios requeridos para la logica en una carpeta /services en el mismo lugar donde estan el resto de carpetas: 

Endpoints

1) Registrar Usuario 
username, correo electronico, password, fecha de nacimiento, nombre de entrenador 
y 3 decks iniciales con las siguientes cartas: 

1º deck - Chespin (Planta) y sus evoluciones Quilladin y Chesnaught.

2x Weedle
2x Pansage
4x Chespin
4x Quilladin
2x Chesnaught
4x Snubbull
2x Granbull
2x Swirlix
1x Snorlax
1x Miltank
2x Bunnelby
4x Poké Ball
2x Poción
2x Cambio
2x Beni
12x Energía Planta
12x Energía Hada

2º deck - Fennekin (Fuego) y sus evoluciones Braixen y Delphox.

2x Slugma
2x Pansear
4x Fennekin
4x Braixen
2x Delphox
4x Poochyena
2x Mightyena
2x Inkay
2x Fletchling
1x Furfrou
1x Furfrou
4x Poké Ball
2x Capturapokémon
2x Cambio
2x Beni
12x Energía Fuego
12x Energía Oscura

3º deck - Froakie (Agua) y sus evoluciones Frogadier y Greninja.

2x Panpour
4x Froakie
4x Frogadier
2x Greninja
2x Clauncher
4x Pawniard
2x Bisharp
2x Honedge
2x Farfetch'd
1x Skitty
1x Bidoof
2x Martillo Demoledor
4x Poké Ball
2x Cambio
2x Beni
12x Energía Agua
12x Energía Metálica

Se deben cargar tanto los 3 decks y todas las cartas usadas sumarlas a la coleccion general de cartas que tiene disponible el Player.

Los mazos deben estar hardcodeados mediante un archivo initial seed 


2) Loguear Usuario

Tiene que llevarse a cabo todo mediante uso de Cookies para guardar sesiones de usuario. 
Recibir logueo con usuario y contraseña y validar que corresponda con algun usuario registrado y realizar la autenticacion mediante la Cookie.
Que el endpoint obviamente al igual que el register devuelva los estados y mensajes correspondientes en ingles

3) Editar nombre de entrenador

4) Obtener datos del Usuario completo
