import assert from "node:assert/strict";
import test from "node:test";
import { paymentIsAllowed } from "../src/policy.js";

const constraints = {
  network: "eip155:8453",
  amountAtomic: "10000",
  recipient: "0x1111111111111111111111111111111111111111",
};

test("allows only the exact configured x402 payment", () => {
  assert.equal(paymentIsAllowed({
    scheme: "exact",
    network: "eip155:8453",
    amount: "10000",
    payTo: "0x1111111111111111111111111111111111111111",
  }, constraints), true);
});

test("rejects price, network, recipient and scheme changes", () => {
  const base = { scheme: "exact", network: "eip155:8453", amount: "10000", payTo: constraints.recipient };
  assert.equal(paymentIsAllowed({ ...base, amount: "10001" }, constraints), false);
  assert.equal(paymentIsAllowed({ ...base, network: "eip155:1" }, constraints), false);
  assert.equal(paymentIsAllowed({ ...base, payTo: "0x2222222222222222222222222222222222222222" }, constraints), false);
  assert.equal(paymentIsAllowed({ ...base, scheme: "upto" }, constraints), false);
});
