export interface PaymentRequirement {
  scheme: string;
  network: string;
  amount: string;
  payTo: string;
}

export interface PaymentConstraints {
  network: string;
  amountAtomic: string;
  recipient: string;
}

export function paymentIsAllowed(requirement: PaymentRequirement, constraints: PaymentConstraints): boolean {
  return requirement.scheme === "exact"
    && requirement.network === constraints.network
    && requirement.amount === constraints.amountAtomic
    && requirement.payTo.toLowerCase() === constraints.recipient.toLowerCase();
}
