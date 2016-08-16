/*******************************************************************************
 * $Id: $
 * Copyright (c) 2009-2010 Tim Tiemens.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * 
 * Contributors:
 *     Tim Tiemens - initial API and implementation
 ******************************************************************************/
package edu.nps.secureshare.shamir.math;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import edu.nps.secureshare.shamir.exceptions.SecretShareException;

public class BigIntUtilities
{


    // ==================================================
    // class static data
    // ==================================================


    // ==================================================
    // class static methods
    // ==================================================

    /**
     * Convert a "human string" into a BigInteger by using the string's
     *   byte[] array.
     * This is NOT the same as new BigInteger("string").
     * 
     * @param in a string like "This is a secret" or "123FooBar"
     * @return BigInteger
     */
    public static BigInteger createFromStringsBytesAsData(final String in)
    {
        BigInteger ret = null;
        byte[] b = in.getBytes();
        ret = new BigInteger(b);
        return ret;
    }
    
    /**
     * @param in the BigInteger whose bytes to use for the String
     * @return String-ified BigInteger.bytes[]
     */
    public static String createStringFromBigInteger(final BigInteger in)
    {
       byte[] b = in.toByteArray();
       String s = new String(b);
       return s;
    }
    

    
    /**
     * @param in biginteger to convert
     * @return the bigintcs:hhhhh-CCCCCC string representation
     */
    public static String createStringMd5CheckSumFromBigInteger(final BigInteger in)
    {
        return BigIntStringChecksum.create(in).toString();

    }


    /**
     * @param value to test
     * @return true if this value is a big-int-checksum string (i.e. starts with "bigintcs:")
     */
    public static boolean couldCreateFromStringMd5CheckSum(String value)
    {
        return BigIntStringChecksum.startsWithPrefix(value);
    }

    /**
     * @param hexStringWithMd5sum the bigintcs:hhhhh-CCCCCC string representation
     * @return the biginteger
     * @throws SecretShareException on error
     */
    public static BigInteger createFromStringMd5CheckSum(final String hexStringWithMd5sum)
    {
        return BigIntStringChecksum.fromString(hexStringWithMd5sum).asBigInteger();
    }




    public static BigInteger createPrimeBigger(BigInteger secret)
    {
        int numbits = secret.bitLength() + 1;
        Random random = new SecureRandom();
        BigInteger ret = BigInteger.probablePrime(numbits, random);
        return ret;
    }



    // ==================================================
    // instance data
    // ==================================================


 
    // ==================================================
    // factories
    // ==================================================

    // ==================================================
    // constructors
    // ==================================================

    // ==================================================
    // public methods
    // ==================================================

    // ==================================================
    // non public methods
    // ==================================================
}
