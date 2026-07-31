import { Boundary } from './classes';

export function createMapArray(flatArray: number[], rowSize: number): number[][] {
  const map: number[][] = [];
  for (let i = 0; i < flatArray.length; i += rowSize) {
    map.push(flatArray.slice(i, rowSize + i));
  }
  return map;
}

export function createBoundaries(
  collisionMap: number[][],
  symbolId: number,
  offset: { x: number; y: number },
  scale: number,
  color?: string
): Boundary[] {
  const boundaries: Boundary[] = [];
  collisionMap.forEach((row, i) => {
    row.forEach((symbol, j) => {
      if (symbol === symbolId) {
        boundaries.push(
          new Boundary({
            position: {
              x: j * Boundary.width * scale + offset.x,
              y: i * Boundary.height * scale + offset.y,
            },
            scale,
            color,
          })
        );
      }
    });
  });
  return boundaries;
}
