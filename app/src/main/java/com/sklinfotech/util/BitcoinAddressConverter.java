package com.sklinfotech.util;

import com.github.kiulian.converter.AddressConverter;

public class BitcoinAddressConverter {

  public String toCashAddress(final String legacyAddress) throws BitcoinAddressInvalidException {
    try {
      return AddressConverter.toCashAddress(legacyAddress);
    } catch (Exception e) {
      throw new BitcoinAddressInvalidException(e);
    }
  }
}
