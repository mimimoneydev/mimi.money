package com.money.mimi.wallet;

public class NftCollection {
    public String address;
    public String name;
    public String symbol;
    public String logoUrl;

    public NftCollection() {}

    public NftCollection(String address, String name, String symbol) {
        this(address, name, symbol, null);
    }

    public NftCollection(String address, String name, String symbol, String logoUrl) {
        this.address = address;
        this.name = name;
        this.symbol = symbol;
        this.logoUrl = logoUrl;
    }
}
