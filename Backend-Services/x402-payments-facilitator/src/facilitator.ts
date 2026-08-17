import { x402Facilitator } from "@x402/core/facilitator";
import { toFacilitatorEvmSigner } from "@x402/evm";
import { ExactEvmScheme } from "@x402/evm/exact/facilitator";
import { UptoEvmScheme } from "@x402/evm/upto/facilitator";
import {
  createPublicClient,
  createWalletClient,
  http,
  type Address,
  type Hex,
} from "viem";
import { privateKeyToAccount } from "viem/accounts";
import { NETWORK_CATALOG, rpcUrl, type AppConfig, type SupportedNetwork } from "./config.js";

export function createFacilitator(config: AppConfig): {
  facilitator: x402Facilitator;
  address: Address;
  rpcByNetwork: Record<string, string>;
} {
  const account = privateKeyToAccount(config.privateKey);
  const facilitator = new x402Facilitator();
  const rpcByNetwork: Record<string, string> = {};

  for (const network of config.networks) {
    const chain = NETWORK_CATALOG[network];
    const url = rpcUrl(network);
    rpcByNetwork[network] = url;
    const publicClient = createPublicClient({ chain, transport: http(url) });
    const walletClient = createWalletClient({ account, chain, transport: http(url) });
    const signer = toFacilitatorEvmSigner({
      address: account.address,
      readContract: args => publicClient.readContract(args as never),
      verifyTypedData: args => publicClient.verifyTypedData(args as never),
      writeContract: args => walletClient.writeContract({ ...args, account, chain } as never),
      sendTransaction: args => walletClient.sendTransaction({ ...args, account, chain }),
      waitForTransactionReceipt: args => publicClient.waitForTransactionReceipt(args),
      getCode: args => publicClient.getCode(args),
    });

    facilitator.register(
      network,
      new ExactEvmScheme(signer, {
        eip6492AllowedFactories: [],
      }),
    );
    facilitator.register(network, new UptoEvmScheme(signer));
  }

  return { facilitator, address: account.address, rpcByNetwork };
}

export function isConfiguredNetwork(value: unknown, config: AppConfig): value is SupportedNetwork {
  return typeof value === "string" && config.networks.includes(value as SupportedNetwork);
}

export function transactionId(response: unknown, fallbackPayload: unknown): string {
  if (response && typeof response === "object") {
    const candidate = (response as { transaction?: unknown }).transaction;
    if (typeof candidate === "string" && candidate.length > 0) return candidate;
  }
  return `payload:${stableHash(JSON.stringify(fallbackPayload))}`;
}

function stableHash(input: string): Hex {
  let hash = 2166136261;
  for (let index = 0; index < input.length; index += 1) {
    hash ^= input.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return `0x${(hash >>> 0).toString(16).padStart(8, "0")}`;
}
