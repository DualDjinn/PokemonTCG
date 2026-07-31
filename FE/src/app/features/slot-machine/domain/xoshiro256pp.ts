export class Xoshiro256pp {
  private s: bigint[];

  constructor() {
    const seed = BigInt(Date.now()) << 32n | BigInt(Math.floor(Math.random() * 0x100000000));
    this.s = [0n, 0n, 0n, 0n];
    let z = seed;
    for (let i = 0; i < 4; i++) {
      z = this.splitmix64(z);
      this.s[i] = z;
    }
  }

  private rotl(x: bigint, k: bigint): bigint {
    return BigInt.asUintN(64, (x << k) | (x >> (64n - k)));
  }

  private splitmix64(z: bigint): bigint {
    z = BigInt.asUintN(64, z + 0x9E3779B97F4A7C15n);
    z = BigInt.asUintN(64, (z ^ (z >> 30n)) * 0xBF58476D1CE4E5B9n);
    z = BigInt.asUintN(64, (z ^ (z >> 27n)) * 0x94D049BB133111EBn);
    z = BigInt.asUintN(64, z ^ (z >> 31n));
    return z;
  }

  next(): bigint {
    const s = this.s;
    const sum = BigInt.asUintN(64, s[0] + s[3]);
    const result = BigInt.asUintN(64, this.rotl(sum, 23n) + s[0]);
    const t = BigInt.asUintN(64, s[1] << 17n);
    s[2] ^= s[0];
    s[3] ^= s[1];
    s[1] ^= s[2];
    s[0] ^= s[3];
    s[2] ^= t;
    s[3] = this.rotl(s[3], 45n);
    return result;
  }

  nextDouble(): number {
    const result = this.next() & 0x1FFFFFFFFFFFFFn;
    return Number(result) / 9007199254740992;
  }
}
