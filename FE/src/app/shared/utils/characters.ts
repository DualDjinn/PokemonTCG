const PORTRAIT_MAP: Record<string, string> = {
  red: 'assets/img/portrait/RedPortrait.png',
  green: 'assets/img/portrait/GreenPortrait.png',
  ruby: 'assets/img/portrait/RubyPortrait.png',
  sapphire: 'assets/img/portrait/SapphirePortrait.png',
  gary: 'assets/img/portrait/BluePortrait.png',
  prima: 'assets/img/portrait/LoreleiPortrait.png',
};

export function getCharacterPortrait(characterId: string): string {
  return PORTRAIT_MAP[characterId] ?? 'assets/img/portrait/RedPortrait.png';
}
