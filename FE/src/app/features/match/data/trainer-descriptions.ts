/** Short descriptions of what each XY1 trainer card does. Keyed by card ID. */
export const TRAINER_DESCRIPTIONS: Record<string, string> = {
  'xy1-115': 'Cassius: devuelve hasta 4 Pokémon del banco a tu mano',
  'xy1-116': 'Evosoda: buscá en tu mazo una evolución de uno de tus Pokémon y ponela sobre él',
  'xy1-117': 'Fairy Garden: los Pokémon de tipo Fairy no gastan energía al retirarse',
  'xy1-118': 'Gran Bola: revisa las primeras 7 cartas del mazo y agarra un Pokémon',
  'xy1-119': 'Hard Charm: reduce en 20 el daño recibido por el Pokémon equipado',
  'xy1-120': 'Max Revive: poné un Pokémon de tu descarte en la cima de tu mazo',
  'xy1-121': 'Muscle Band: el Pokémon equipado hace +20 de daño al activo rival',
  'xy1-122': 'Profesor Plátano: descarta tu mano y roba 7 cartas',
  'xy1-123': 'Carta del Profesor: busca 2 cartas de Energía Básica en el mazo',
  'xy1-124': 'Carta Roja: el rival mezcla su mano y roba 4 cartas',
  'xy1-125': 'Patines: lanza una moneda — cara: roba 3 cartas',
  'xy1-126': 'Círculo Oscuro: los Pokémon Siniestro y Psíquico ignoran la Resistencia',
  'xy1-127': 'Shauna: mezcla tu mano y roba 5 cartas',
  'xy1-128': 'Súper Poción: cura 60 PS de un Pokémon (descarta 1 Energía)',
  'xy1-129': 'Comando Flare: descarta 1 Energía del Pokémon activo rival',
};

export function getTrainerDescription(cardId: string): string {
  return TRAINER_DESCRIPTIONS[cardId] ?? '';
}
