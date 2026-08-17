import "dotenv/config";
import { getAddress, isAddress } from "viem";
import { privateKeyToAccount } from "viem/accounts";

function integer(name: string, fallback: number, minimum: number): number {
  const raw = process.env[name];
  const value = raw ? Number(raw) : fallback;
  if (!Number.isInteger(value) || value < minimum) throw new Error(`${name} is invalid`);
  return value;
}

export function loadConfig() {
  const privateKey = process.env.PAYER_PRIVATE_KEY ?? "";
  if (!/^0x[0-9a-fA-F]{64}$/.test(privateKey)) {
    throw new Error("PAYER_PRIVATE_KEY must be a 32-byte 0x-prefixed private key");
  }

  const internalToken = process.env.INTERNAL_TOKEN ?? "";
  if (internalToken.length < 32) throw new Error("INTERNAL_TOKEN must contain at least 32 characters");

  const seller = process.env.AGENTICOUS_SELLER_ADDRESS ?? "";
  if (!isAddress(seller) || /^0x0{40}$/i.test(seller)) {
    throw new Error("AGENTICOUS_SELLER_ADDRESS is invalid");
  }

  const paymentNetwork = (process.env.PAYMENT_NETWORK ?? "eip155:8453") as `${string}:${string}`;
  if (!/^eip155:\d+$/.test(paymentNetwork)) throw new Error("PAYMENT_NETWORK is invalid");

  const amount = process.env.PAYMENT_AMOUNT_ATOMIC ?? "10000";
  if (!/^\d+$/.test(amount) || BigInt(amount) <= 0n) throw new Error("PAYMENT_AMOUNT_ATOMIC is invalid");

  const agenticous = new URL(process.env.AGENTICOUS_URL ?? "https://agenticous.mimi.money");
  if (!["https:", "http:"].includes(agenticous.protocol)) throw new Error("AGENTICOUS_URL must be HTTP(S)");

  const account = privateKeyToAccount(privateKey as `0x${string}`);
  return {
    host: process.env.HOST ?? "127.0.0.1",
    port: integer("PORT", 4411, 1),
    internalToken,
    payerPrivateKey: privateKey as `0x${string}`,
    payerAddress: account.address,
    agenticousUrl: agenticous.toString().replace(/\/$/, ""),
    sellerAddress: getAddress(seller),
    paymentNetwork,
    paymentAmountAtomic: amount,
    requestTimeoutMs: integer("REQUEST_TIMEOUT_MS", 30_000, 1_000),
  };
}
