package com.money.mimi.wallet;

public class TokenInfo {
    public String address;
    public String symbol;
    public String name;
    public int decimals;
    public String logoUrl;

    public TokenInfo() {}

    public TokenInfo(String address, String symbol, String name, int decimals) {
        this(address, symbol, name, decimals, null);
    }

    public TokenInfo(String address, String symbol, String name, int decimals, String logoUrl) {
        this.address = address;
        this.symbol = symbol;
        this.name = name;
        this.decimals = decimals;
        this.logoUrl = logoUrl;
    }
}
