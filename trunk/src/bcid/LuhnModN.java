package bcid;

//import com.modp.checkdigits.CheckDigit;

import java.nio.charset.Charset;

/**
 * Check Digit using LuhnModN algorithm from http://en.wikipedia.org/wiki/Luhn_mod_N_algorithm
 */
public class LuhnModN {
    Charset cset = Charset.forName("US-ASCII");

    // code characters for values 0..63
    static private char[] alphabet =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
                    .toCharArray();

    // lookup table for converting base64 characters to value in range 0..63
    static private byte[] codes = new byte[256];

    static {
        for (int i = 0; i < 256; i++) codes[i] = -1;
        for (int i = 'A'; i <= 'Z'; i++) codes[i] = (byte) (i - 'A');
        for (int i = 'a'; i <= 'z'; i++) codes[i] = (byte) (26 + i - 'a');
        for (int i = '0'; i <= '9'; i++) codes[i] = (byte) (52 + i - '0');
        codes['+'] = 62;
        codes['/'] = 63;
    }

    public int NumberOfValidInputCharacters(String input) {
        return input.length();
    }

    public int CodePointFromCharacter(char character) {
        return codes[character];
    }

    public char CharacterFromCodePoint(int codePoint) {
        return alphabet[codePoint];
    }

    public char GenerateCheckCharacter(String input) {
        int factor = 2;
        int sum = 0;
        int n = NumberOfValidInputCharacters(input);

        // Starting from the right and working leftwards is easier since
        // the initial "factor" will always be "2"
        for (int i = input.length() - 1; i >= 0; i--) {
            int codePoint = CodePointFromCharacter(input.charAt(i));
            int addend = factor * codePoint;

            // Alternate the "factor" that each "codePoint" is multiplied by
            factor = (factor == 2) ? 1 : 2;

            // Sum the digits of the "addend" as expressed in base "n"
            addend = (addend / n) + (addend % n);
            sum += addend;
        }

        // Calculate the number that must be added to the "sum"
        // to make it divisible by "n"
        int remainder = sum % n;
        int checkCodePoint = (n - remainder) % n;

        return CharacterFromCodePoint(checkCodePoint);
    }

    public String encode(String input) {
        return input + GenerateCheckCharacter(input);
    }

    public boolean verify(String input) {

        int factor = 1;
        int sum = 0;
        int n = NumberOfValidInputCharacters(input) - 1;

        // Starting from the right, work leftwards
        // Now, the initial "factor" will always be "1"
        // since the last character is the check character
        for (int i = input.length() - 1; i >= 0; i--) {
            int codePoint = CodePointFromCharacter(input.charAt(i));
            int addend = factor * codePoint;

            // Alternate the "factor" that each "codePoint" is multiplied by
            factor = (factor == 2) ? 1 : 2;

            // Sum the digits of the "addend" as expressed in base "n"
            addend = (addend / n) + (addend % n);
            sum += addend;
        }

        int remainder = sum % n;

        return (remainder == 0);
    }

    /**
     * String the last character from the String, which is the checkDigit
     * @param digits
     * @return
     */
    public String getData(String digits) {
        return digits.substring(0, digits.length() -1);
    }

}
