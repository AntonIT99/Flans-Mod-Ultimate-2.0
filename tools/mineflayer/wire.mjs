// The small subset of FriendlyByteBuf used by Forge/NeoForge negotiation.
export function varint(value) {
  const bytes = [];
  let remaining = value >>> 0;
  do {
    const byte = remaining & 127;
    remaining >>>= 7;
    bytes.push(byte | (remaining ? 128 : 0));
  } while (remaining);
  return Buffer.from(bytes);
}

export function utf(value) {
  const bytes = Buffer.from(value, "utf8");
  return Buffer.concat([varint(bytes.length), bytes]);
}

export function array(values, encode) {
  return Buffer.concat([varint(values.length), ...values.map(encode)]);
}

export class Reader {
  constructor(buffer) { this.buffer = buffer; this.offset = 0; }
  bytes(length) {
    if (!Number.isInteger(length) || length < 0 || this.offset + length > this.buffer.length) throw new Error("Truncated loader payload");
    const value = this.buffer.subarray(this.offset, this.offset + length);
    this.offset += length;
    return value;
  }
  byte() { return this.bytes(1)[0]; }
  varint() {
    let value = 0;
    for (let shift = 0; shift < 35; shift += 7) {
      const byte = this.byte();
      value |= (byte & 127) << shift;
      if (!(byte & 128)) return value;
    }
    throw new Error("Invalid loader VarInt");
  }
  utf() { return this.bytes(this.varint()).toString("utf8"); }
  array(read) {
    const length = this.varint();
    if (length < 0 || length > 1_000_000) throw new Error("Invalid loader collection size");
    return Array.from({ length }, () => read(this));
  }
  rest() { return this.bytes(this.buffer.length - this.offset); }
  end() { if (this.offset !== this.buffer.length) throw new Error("Unexpected trailing loader data; check the pinned loader version"); }
}

export function wrapForge(channel, data) {
  return Buffer.concat([utf(channel), varint(data.length), data]);
}
