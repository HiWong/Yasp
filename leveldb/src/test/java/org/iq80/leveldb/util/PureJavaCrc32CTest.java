/*
 * Copyright (C) 2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iq80.leveldb.util;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.function.Function;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.iq80.leveldb.util.PureJavaCrc32C.mask;
import static org.iq80.leveldb.util.PureJavaCrc32C.unmask;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PureJavaCrc32CTest {
  private static int computeCrc(byte[] data) {
    PureJavaCrc32C crc = new PureJavaCrc32C();
    crc.update(data, 0, data.length);
    return crc.getIntValue();
  }

  private static byte[] arrayOf(int size, byte value) {
    byte[] result = new byte[size];
    Arrays.fill(result, value);
    return result;
  }

  @SuppressWarnings("ConstantConditions")
  private static byte[] arrayOf(int size, Function<Integer, Byte> generator) {
    byte[] result = new byte[size];
    for (int i = 0; i < result.length; ++i) {
      result[i] = generator.apply(i);
    }

    return result;
  }

  private static byte[] arrayOf(int[] bytes) {
    byte[] result = new byte[bytes.length];
    for (int i = 0; i < result.length; ++i) {
      result[i] = (byte) bytes[i];
    }

    return result;
  }

  @Test(dataProvider = "crcs")
  public void testCrc(int expectedCrc, byte[] data) {
    assertEquals(expectedCrc, computeCrc(data));
  }

  @DataProvider(name = "crcs")
  public Object[][] data() {
    return new Object[][] {
        new Object[] {0x8a9136aa, arrayOf(32, (byte) 0)},
        new Object[] {0x62a8ab43, arrayOf(32, (byte) 0xff)},
        new Object[] {0x46dd794e, arrayOf(32, new Function<Integer, Byte>() {
          @Override
          public Byte apply(Integer integer) {
            return (byte) integer.intValue();
          }
        })},
        new Object[] {0x113fdb5c, arrayOf(32, new Function<Integer, Byte>() {
          @Override
          public Byte apply(Integer integer) {
            return (byte) (31 - integer);
          }
        })},
        new Object[] {0xd9963a56, arrayOf(new int[] {
            0x01, 0xc0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00,
            0x00, 0x00, 0x00, 0x14, 0x00, 0x00, 0x00, 0x18, 0x28, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00})}
    };
  }

  @Test
  public void testProducesDifferentCrcs() throws UnsupportedEncodingException {
    assertFalse(computeCrc("a".getBytes(US_ASCII)) == computeCrc("foo".getBytes(US_ASCII)));
  }

  @Test
  public void testComposes() throws UnsupportedEncodingException {
    PureJavaCrc32C crc = new PureJavaCrc32C();
    crc.update("hello ".getBytes(US_ASCII), 0, 6);
    crc.update("world".getBytes(US_ASCII), 0, 5);

    assertEquals(crc.getIntValue(), computeCrc("hello world".getBytes(US_ASCII)));
  }

  @Test
  public void testMask() throws UnsupportedEncodingException {
    PureJavaCrc32C crc = new PureJavaCrc32C();
    crc.update("foo".getBytes(US_ASCII), 0, 3);

    assertEquals(crc.getMaskedValue(), mask(crc.getIntValue()));
    assertFalse(crc.getIntValue() == crc.getMaskedValue(), "crc should not match masked crc");
    assertFalse(
        crc.getIntValue() == mask(crc.getMaskedValue()),
        "crc should not match double masked crc"
    );
    assertEquals(crc.getIntValue(), unmask(crc.getMaskedValue()));
    assertEquals(crc.getIntValue(), unmask(unmask(mask(crc.getMaskedValue()))));
  }
}
