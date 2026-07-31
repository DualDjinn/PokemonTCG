export interface NpcSpawn {
  name: string;
  col: number;
  row: number;
  dialogue?: string;
  dialogues?: string[];
  menuOptions?: string[];
  /**
   * Carta secreta que el NPC regala la primera vez que se habla con él.
   * El backend valida el mapeo NPC→carta y registra el reclamo, así que
   * este id solo indica que el NPC tiene recompensa (debe coincidir con
   * el mapeo del backend para que el reclamo funcione).
   */
  secretRewardCardId?: string;
  randomDialogue?: boolean;
  /**
   * Si está presente, el NPC es desafiable: al elegir "Enfrentamiento" se crea
   * un bot match con esta dificultad y se navega directo a la batalla.
   */
  botDifficulty?: 'EASY' | 'MEDIUM' | 'HARD';
  /**
   * Tutorial interactivo con video. Si está presente, el NPC muestra un botón
   * "Tutorial" que dispara el flujo: diálogo → video → diálogo → recompensas.
   */
  tutorial?: { videoSrc: string };
}

export interface LocationConfig {
  name: string;
  mapImage: string;
  foregroundImage?: string;
  spawnTile: { col: number; row: number };
  mapGridSize: number;
  collisionSymbol: number;
  zones: Array<{
    type: 'enter' | 'leave';
    symbol: number;
    destination: string;
    spawnTileDelta: { dcol: number; drow: number };
    dataKey?: string;
  }>;
  npcs?: NpcSpawn[];
  /** Efecto ambiental para mapas exteriores (hojas de otoño y viento). */
  ambientEffect?: 'autumnLeaves';
  /**
   * Decoraciones animadas (fuentes, flores) que se dibujan sobre el mapa
   * estático sin recargarlo. Cada entrada es la esquina superior-izquierda
   * (col/row) del bounding-box del sprite en tiles de 28px.
   */
  decorations?: AnimatedDecorationSpawn[];
}

export interface AnimatedDecorationSpawn {
  kind: 'fountain' | 'flower';
  col: number;
  row: number;
}

export const LOCATIONS_CONFIG: Record<string, LocationConfig> = {
  ciudadPrincipal: {
    name: 'Ciudad Principal',
    mapImage: 'assets/maps/CiudadPrincipal.png',
    foregroundImage: 'assets/maps/Foreground/CiudadPrincipalForeGroundV2.png',
    ambientEffect: 'autumnLeaves',
    spawnTile: { col: 60, row: 45 },
    mapGridSize: 120,
    collisionSymbol: 46650,
    zones: [
      {
        type: 'enter',
        symbol: 52450,
        destination: 'preLan',
        spawnTileDelta: { dcol: 0, drow: 0 },
        dataKey: 'enterPreLan',
      },
      {
        type: 'enter',
        symbol: 47403,
        destination: 'tutorial',
        spawnTileDelta: { dcol: 33, drow: 29 },
        dataKey: 'enterTutorial',
      },
    ],
    decorations: [
      { kind: 'fountain', col: 33, row: 38 },
      { kind: 'flower', col: 76, row: 21 },
      { kind: 'flower', col: 88, row: 21 },
      { kind: 'flower', col: 84, row: 29 },
      { kind: 'flower', col: 88, row: 33 },
      { kind: 'flower', col: 31, row: 37 },
      { kind: 'flower', col: 39, row: 37 },
      { kind: 'flower', col: 29, row: 41 },
      { kind: 'flower', col: 41, row: 41 },
      { kind: 'flower', col: 52, row: 44 },
      { kind: 'flower', col: 56, row: 44 },
      { kind: 'flower', col: 74, row: 44 },
      { kind: 'flower', col: 78, row: 44 },
      { kind: 'flower', col: 31, row: 45 },
      { kind: 'flower', col: 39, row: 45 },
    ],
    npcs: [
      {
        name: 'Don Alfajor',
        col: 52,
        row: 25,
        dialogue: 'Mis alfajores estan mejores que los Churros',
        menuOptions: ['Hablar', 'Salir'],
        secretRewardCardId: 'pokesecret-8',
      },
      {
        name: 'Sofia',
        col: 35,
        row: 42,
        dialogue: 'Al final del cuatri siempre le hago 3 preguntas a mis chiques',
        menuOptions: ['Hablar', 'Salir'],
        secretRewardCardId: 'pokesecret-9',
      },
      {
        name: 'Mr. Brightside',
        col: 43,
        row: 22,
        dialogues: ['','','','','','','','',''],
        randomDialogue: true,
        menuOptions: ['Hablar', 'Salir'],
      },
      {
        name: 'Ramiro',
        col: 39,
        row: 31,
        dialogue: 'Chicos no toquen las diapositivas de Hernan',
        menuOptions: ['Hablar', 'Salir'],
        secretRewardCardId: 'pokesecret-1',
      },
      {
        name: 'Juanse',
        col: 76,
        row: 43,
        dialogue: 'Me vine a este pueblo para hacer una actividad mas tranquila que el futbol',
        menuOptions: ['Hablar', 'Salir'],
        secretRewardCardId: 'pokesecret-7',
      },
      {
        name: 'Hernan',
        col: 66,
        row: 27,
        dialogue: 'Esto ya lo tendrian que saber',
        menuOptions: ['Hablar', 'Salir'],
        secretRewardCardId: 'pokesecret-2',
      },
    ],
  },
  lan: {
    name: 'Lan',
    mapImage: 'assets/maps/Lan.png',
    spawnTile: { col: 15, row: 15 },
    mapGridSize: 70,
    collisionSymbol: 46745,
    zones: [
      {
        type: 'leave',
        symbol: 34670,
        destination: 'preLan',
        spawnTileDelta: { dcol: 25, drow: -21 },
      },
    ],
    npcs: [
      { name: 'Profesor Oak', col: 34.15, row: 17, dialogue: 'Confia en el corazon de las cartas. A no, eso es de mi otro trabajo', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], secretRewardCardId: 'pokesecret-10' },
      { name: 'Gandalf Holmes', col: 26, row: 27, dialogue: '¿Me puedes decir que dice esta carta? No traje mis anteojos', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], botDifficulty: 'MEDIUM' },
      { name: 'Johto', col: 11, row: 25, dialogue: 'No pude ser estrella de Kpop, asi que me vine a jugar cartas', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], secretRewardCardId: 'pokesecret-6', botDifficulty: 'MEDIUM' },
      { name: 'Rick Grimes', col: 26, row: 31, dialogue: 'Vine a investigar un contrabando de cartas, pero por mientras podria jugar una partida', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], botDifficulty: 'MEDIUM' },
      { name: 'Kanto', col: 11, row: 33, dialogue: 'Mejor juego aqui o la seño me quita mis cartas', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], botDifficulty: 'MEDIUM' },
      { name: 'Billy Morgan', col: 42, row: 25, dialogue: 'Por suerte pude entrar, la dueña no me queria dejar entrar por ser Ing. en Sistemas', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], botDifficulty: 'MEDIUM' },
      { name: 'Iris', col: 42, row: 33, dialogue: 'Parece que aqui yo soy la unica que sabe jugar', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], botDifficulty: 'MEDIUM' },
      { name: 'Johnny Lawrence', col: 57, row: 27, dialogue: '¡Golpear primero, golpear fuerte, sin piedad!', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], botDifficulty: 'MEDIUM' },
      { name: 'Elvis', col: 57, row: 31, dialogue: 'El ritmo es algo que se tiene o no se tiene, pero cuando lo tienes, lo tienes todo', menuOptions: ['Enfrentamiento', 'Hablar', 'Salir'], botDifficulty: 'MEDIUM' },
    ],
  },
  preLan: {
    name: 'Pre-Lan',
    mapImage: 'assets/maps/preLan.png',
    spawnTile: { col: 33, row: 40 },
    mapGridSize: 80,
    collisionSymbol: 46745,
    zones: [
      {
        type: 'enter',
        symbol: 34670,
        destination: 'lan',
        spawnTileDelta: { dcol: 1, drow: -1 },
      },
      {
        type: 'leave',
        symbol: 46925,
        destination: 'ciudadPrincipal',
        spawnTileDelta: { dcol: -29, drow: -15 },
      },
      {
        type: 'enter',
        symbol: 6774,
        destination: 'secret',
        spawnTileDelta: { dcol: 0, drow: 0 },
        dataKey: 'enterBuildSecret',
      },
    ],
    npcs: [
      { name: 'Blanca', col: 25, row: 18, dialogue: 'Espero que no vengan los de ingenieria, son todos unos vagos', menuOptions: ['comprar', 'hablar', 'salir'] },
      { name: 'Sinnoh', col: 27.5, row: 33, dialogue: 'Espero que salga la nueva expansion, va a estar muy buena', menuOptions: ['hablar', 'salir'] },
      { name: 'Tesalia', col: 33, row: 33, dialogue: 'No me gusta lo nuevo, antes las expansiones eran mejores', menuOptions: ['hablar', 'salir'] },
      { name: 'Pelon', col: 60.5, row: 30, dialogue: 'Necesito ganar para poder hacerme el ingerto de pelo', menuOptions: ['hablar', 'salir'] },
      { name: 'Madre', col: 60.5, row: 26, dialogue: 'En algun momento esta maquina va a pagar', menuOptions: ['hablar', 'salir'] },
      { name: 'Cientifico', col: 60.5, row: 34, dialogue: 'Las probabilidades no mienten, en la siguiente jugada gano si o si', menuOptions: ['hablar', 'salir'] },
      { name: 'La Rubia', col: 17, row: 28, dialogue: 'No puede ser que ya ganara un chilion de dolares', menuOptions: ['hablar', 'salir'] },
      { name: 'Chico Turbio', col: 17, row: 26, dialogue: 'Tengo que recuperar la pension de mi Madre', menuOptions: ['hablar', 'salir'] },
      { name: 'Compradora Compulsiva', col: 38, row: 19, dialogue: 'Me quiero llevar todo, hay muchas ofertas', menuOptions: ['hablar', 'salir'] },
      { name: 'Rubio Tacaño', col: 43, row: 27, dialogue: '¿Sobres de pokemon en esta economia?', menuOptions: ['hablar', 'salir'] },
      { name: 'Revendedor', col: 50, row: 37, dialogue: 'Con estas cartas me hago millonario, STONKS!!!', menuOptions: ['hablar', 'salir'] },
    ],
  },
  secret: {
    name: 'Secret',
    mapImage: 'assets/maps/secretLevelSantoro.png',
    spawnTile: { col: 25, row: 26 },
    mapGridSize: 70,
    collisionSymbol: 7143,
    zones: [
      {
        type: 'leave',
        symbol: 6775,
        destination: 'preLan',
        spawnTileDelta: { dcol: 27, drow: 0 },
      },
    ],
    npcs: [
      { name: 'Santoro', col: 40, row: 19, dialogue: 'Que haces cacho, menos mal que me encontraste, la que administra la tienda me encerró porque según ella ahuyento a los clientes', menuOptions: ['Hablar', 'Salir'], secretRewardCardId: 'pokesecret-3' },
    ],
  },
  tutorial: {
    name: 'Tutorial',
    mapImage: 'assets/maps/tutorial.png',
    spawnTile: { col: 0, row: 0 },
    mapGridSize: 70,
    collisionSymbol: 36432,
    zones: [
      {
        type: 'leave',
        symbol: 39357,
        destination: 'ciudadPrincipal',
        spawnTileDelta: { dcol: 0, drow: 0 },
      },
    ],
    npcs: [
      {
        name: 'Fabio y Joaco',
        col: 32,
        row: 22,
        dialogues: [
          'Fabio: Joaco se largo a llorar porque no entendieron el tema de la clase pasada',
          'Joaco: Chicos tienen que estudiar o no les va a ir bien en el parcial',
        ],
        tutorial: { videoSrc: 'assets/video/pokemonTutorial.webm' },
        menuOptions: ['Hablar', 'Salir'],
      },
    ],
  },
};
