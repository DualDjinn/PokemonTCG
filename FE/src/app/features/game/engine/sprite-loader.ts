export const PLAYER_SPRITES_CONFIG: Record<string, { down: string; up: string; left: string; right: string }> = {
  red_rojo: {
    down: 'assets/img/character/redRed/redDOWN.png',
    up: 'assets/img/character/redRed/redUp.png',
    left: 'assets/img/character/redRed/redLeft.png',
    right: 'assets/img/character/redRed/redRight.png',
  },
  red_verde: {
    down: 'assets/img/character/redGreen/redGreenDown.png',
    up: 'assets/img/character/redGreen/redGreenUp.png',
    left: 'assets/img/character/redGreen/redGreenLeft.png',
    right: 'assets/img/character/redGreen/redGreenRight.png',
  },
  red_violeta: {
    down: 'assets/img/character/RedViolet/redVioletDown.png',
    up: 'assets/img/character/RedViolet/redVioletUp.png',
    left: 'assets/img/character/RedViolet/redVioletLeft.png',
    right: 'assets/img/character/RedViolet/redVioletRight.png',
  },
  green_rojo: {
    down: 'assets/img/character/greenRed/greenRedDown.png',
    up: 'assets/img/character/greenRed/greenRedUp.png',
    left: 'assets/img/character/greenRed/greenRedLeft.png',
    right: 'assets/img/character/greenRed/greenRedRight.png',
  },
  green_verde: {
    down: 'assets/img/character/greenGreen/greenGreenDown.png',
    up: 'assets/img/character/greenGreen/greenGreenUp.png',
    left: 'assets/img/character/greenGreen/greenGreenLeft.png',
    right: 'assets/img/character/greenGreen/greenGreenRight.png',
  },
  green_violeta: {
    down: 'assets/img/character/greenViolet/greenVioletDown.png',
    up: 'assets/img/character/greenViolet/greenVioletUp.png',
    left: 'assets/img/character/greenViolet/greenVioletLeft.png',
    right: 'assets/img/character/greenViolet/greenVioletRight.png',
  },
  ruby_rojo: {
    down: 'assets/img/character/rubyRed/rubyRedDown.png',
    up: 'assets/img/character/rubyRed/rubyUp.png',
    left: 'assets/img/character/rubyRed/rubyRedLeft.png',
    right: 'assets/img/character/rubyRed/rubyRedRight.png',
  },
  ruby_verde: {
    down: 'assets/img/character/rubyGreen/rubyGreenDown.png',
    up: 'assets/img/character/rubyGreen/rubyGreenUp.png',
    left: 'assets/img/character/rubyGreen/rubyGreenLeft.png',
    right: 'assets/img/character/rubyGreen/rubyGreenRight.png',
  },
  ruby_violeta: {
    down: 'assets/img/character/rubyViolet/rubyVioletDown.png',
    up: 'assets/img/character/rubyViolet/rubyVioletUp.png',
    left: 'assets/img/character/rubyViolet/rubyVioletLeft.png',
    right: 'assets/img/character/rubyViolet/rubyVioletRight.png',
  },
  sapphire_rojo: {
    down: 'assets/img/character/sapphireRed/sapphireRedDown.png',
    up: 'assets/img/character/sapphireRed/sapphireRedUp.png',
    left: 'assets/img/character/sapphireRed/sapphireRedLeft.png',
    right: 'assets/img/character/sapphireRed/sapphireRedRight.png',
  },
  sapphire_verde: {
    down: 'assets/img/character/sapphireGreen/sapphireGreenDown.png',
    up: 'assets/img/character/sapphireGreen/sapphireGreenUp.png',
    left: 'assets/img/character/sapphireGreen/sapphireGreenLeft.png',
    right: 'assets/img/character/sapphireGreen/sapphireGreenRight.png',
  },
  sapphire_violeta: {
    down: 'assets/img/character/sapphireViolet/sapphireVioletDown.png',
    up: 'assets/img/character/sapphireViolet/sapphireVioletUp.png',
    left: 'assets/img/character/sapphireViolet/sapphireVioletLeft.png',
    right: 'assets/img/character/sapphireViolet/sapphireVioletRight.png',
  },
  gary: {
    down: 'assets/img/character/gary/garyDown.png',
    up: 'assets/img/character/gary/garyUp.png',
    left: 'assets/img/character/gary/garyLeft.png',
    right: 'assets/img/character/gary/garyRight.png',
  },
  prima: {
    down: 'assets/img/character/prima/primaDown.png',
    up: 'assets/img/character/prima/primaUp.png',
    left: 'assets/img/character/prima/primaLeft.png',
    right: 'assets/img/character/prima/primaRight.png',
  },
};

export function loadPlayerSprites(key: string = 'red_rojo'): { down: HTMLImageElement; up: HTMLImageElement; left: HTMLImageElement; right: HTMLImageElement } {
  const config = PLAYER_SPRITES_CONFIG[key] || PLAYER_SPRITES_CONFIG['red_rojo'];
  const down = new Image(); down.src = config['down'];
  const up = new Image(); up.src = config['up'];
  const left = new Image(); left.src = config['left'];
  const right = new Image(); right.src = config['right'];
  return { down, up, left, right };
}

export const NPC_SPRITES_CONFIG: Record<string, string> = {
  'Don Alfajor': 'assets/img/character/NPC/alfajoresFrente.png',
  'Elvis': 'assets/img/character/NPC/elvisIzquierda.png',
  'Johnny Lawrence': 'assets/img/character/NPC/cobraKaiIzquierda.png',
  'Sofia': 'assets/img/character/NPC/SofiaFrente.png',
  'Fabio y Joaco': 'assets/img/character/NPC/Joaco&Fabio.png',
  Blanca: 'assets/img/character/NPC/blancaFront.png',
  Sinnoh: 'assets/img/character/NPC/gordoAzulDerecha.png',
  Tesalia: 'assets/img/character/NPC/gordoRojoIzquierda.png',
  'Profesor Oak': 'assets/img/character/NPC/oakFront.png',
  'Gandalf Holmes': 'assets/img/character/NPC/viejoIzquierda.png',
  Johto: 'assets/img/character/NPC/chicoAzulDerecha.png',
  'Rick Grimes': 'assets/img/character/NPC/policiaIzquierda.png',
  Kanto: 'assets/img/character/NPC/chicoTerryDerecha.png',
  'Billy Morgan': 'assets/img/character/NPC/billyDerecha.png',
  Iris: 'assets/img/character/NPC/chicaAzulDerecha.png',
  Pelon: 'assets/img/character/NPC/PelonDerecha.png',
  Madre: 'assets/img/character/NPC/MadreDerecha.png',
  Cientifico: 'assets/img/character/NPC/CientificoDerecha.png',
  'La Rubia': 'assets/img/character/NPC/RubiaIzquierda.png',
  'Chico Turbio': 'assets/img/character/NPC/TenderoRojoIzquierda.png',
  'Compradora Compulsiva': 'assets/img/character/NPC/CompradoraCompulsiva.png',
  'Rubio Tacaño': 'assets/img/character/NPC/RubioMean.png',
  'Revendedor':              'assets/img/character/NPC/Revendedor.png',
  'Santoro':                 'assets/img/character/NPC/Santoro.png',
  'Mr. Brightside':          'assets/img/character/NPC/Mr. Brightside.png',
  'Ramiro':                  'assets/img/character/NPC/Ramiro.png',
  'Juanse':                  'assets/img/character/NPC/juanseFrente.png',
  'Hernan':                  'assets/img/character/NPC/Hernan.png',
};

export function loadNpcSprite(key: string): HTMLImageElement {
  const path = NPC_SPRITES_CONFIG[key];
  const image = new Image();
  image.src = path;
  return image;
}

export function loadMapImage(imagePath: string): HTMLImageElement {
  const image = new Image();
  image.src = imagePath;
  return image;
}
