package com.ghostxx.algotools.utils;

import java.util.concurrent.Callable;

public final class HashSearcherTasks {

    private HashSearcherTasks() {
        // Utility class, not meant to be instantiated
    }

    public static boolean isPrintableChar(byte b) {
        return (b >= 32 && b < 127) || (b < 0); // Allow negative byte values as they can be part of UTF-8 valid chars
    }

    public static class MD5Searcher implements Callable<String> {
        private final String hash;
        private final byte[] data;

        public MD5Searcher(String hash, byte[] data) {
            this.hash = hash;
            this.data = data;
        }

        @Override
        public String call() {
            int start = 0;
            while (start < data.length) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                StringBuilder possibleText = new StringBuilder();
                int i = start;
                while (i < data.length && isPrintableChar(data[i])) {
                    possibleText.append((char) data[i]);
                    i++;
                }
                if (possibleText.length() > 0) {
                    String currentHash = CryptoUtils.calculateMD5(possibleText.toString());
                    if (hash.equalsIgnoreCase(currentHash)) {
                        return possibleText.toString();
                    }
                }
                start = i + 1;
            }
            return null;
        }
    }

    public static class SHA1Searcher implements Callable<String> {
        private final String hash;
        private final byte[] data;

        public SHA1Searcher(String hash, byte[] data) {
            this.hash = hash;
            this.data = data;
        }

        @Override
        public String call() {
            int start = 0;
            while (start < data.length) {
                if (Thread.currentThread().isInterrupted()) return null;
                StringBuilder possibleText = new StringBuilder();
                int i = start;
                while (i < data.length && isPrintableChar(data[i])) {
                    possibleText.append((char) data[i]);
                    i++;
                }
                if (possibleText.length() > 0) {
                    String currentHash = CryptoUtils.calculateSHA1(possibleText.toString());
                    if (hash.equalsIgnoreCase(currentHash)) {
                        return possibleText.toString();
                    }
                }
            start = i + 1;
        }
            return null;
        }
    }

    public static class SHA256Searcher implements Callable<String> {
        private final String hash;
        private final byte[] data;

        public SHA256Searcher(String hash, byte[] data) {
            this.hash = hash;
            this.data = data;
        }

        @Override
        public String call() {
            int start = 0;
            while (start < data.length) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                StringBuilder possibleText = new StringBuilder();
                int i = start;
                while (i < data.length && isPrintableChar(data[i])) {
                    possibleText.append((char) data[i]);
                    i++;
                }

                if (possibleText.length() > 0) {
                    String text = possibleText.toString();
                    String currentHash = CryptoUtils.calculateSHA256(text);
                    if (hash.equalsIgnoreCase(currentHash)) {
                        return text;
                    }
                }
                start = i + 1;
            }
        return null;
        }
    }
} 