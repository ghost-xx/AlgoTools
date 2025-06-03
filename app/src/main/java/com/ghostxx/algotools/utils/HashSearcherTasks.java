package com.ghostxx.algotools.utils;

import java.util.concurrent.Callable;

/**
 * HashSearcherTasks 包含用于在字节数组中搜索哈希原文的 Callable 任务。
 * 这些任务通常由 CryptoUtils 中的 crackXXX 方法并发执行。
 */
public final class HashSearcherTasks {

    private HashSearcherTasks() {
        // 工具类，不应被实例化
    }

    /**
     * 内部辅助类，用于封装从字节数组中提取出的文本字符串以及下次搜索的起始索引。
     */
    private static class ExtractedTextResult {
        final String text;         // 提取出的文本字符串
        final int nextIndex;    // 下一个搜索应该开始的索引（即停止提取的字符的索引，或如果到达末尾则是 data.length）

        ExtractedTextResult(String text, int nextIndex) {
            this.text = text;
            this.nextIndex = nextIndex;
        }
    }

    /**
     * 检查给定的字节是否可以可被……认为打印字符。
     * @param b 要检查的字节
     * @return 如果字节是可打印字符则返回 true，否则返回 false。
     *         允许负字节值，因为它们可能是有效的UTF-8字符的一部分。
     */
    public static boolean isPrintableChar(byte b) {
        return (b >= 32 && b < 127) || (b < 0); 
    }

    /**
     * 从数据数组中提取一个连续的可打印字符序列。
     * @param data 要搜索的字节数组。
     * @param searchStart 开始搜索的索引。
     * @return 一个 ExtractedTextResult 对象，包含提取的字符串和下一个非可打印字符的索引
     *         （或者如果到达数组末尾，则是 data.length）。
     */
    private static ExtractedTextResult extractNextPossibleText(byte[] data, int searchStart) {
        StringBuilder sb = new StringBuilder();
        int currentIndex = searchStart;
        while (currentIndex < data.length && isPrintableChar(data[currentIndex])) {
            sb.append((char) data[currentIndex]);
            currentIndex++;
        }
        return new ExtractedTextResult(sb.toString(), currentIndex);
    }

    /**
     * MD5Searcher 是一个 Callable 任务，用于在给定的字节数据块中搜索与目标MD5哈希匹配的原文。
     */
    public static class MD5Searcher implements Callable<String> {
        private final String hash;    // 目标MD5哈希值
        private final byte[] data;    // 要搜索的字节数据块

        public MD5Searcher(String hash, byte[] data) {
            this.hash = hash;
            this.data = data;
        }

        @Override
        public String call() {
            int currentSearchStart = 0; // 当前搜索的起始位置
            while (currentSearchStart < data.length) {
                if (Thread.currentThread().isInterrupted()) { // 检查线程是否被中断
                    return null;
                }
                
                // 提取下一个可能的文本字符串及其结束位置
                ExtractedTextResult extraction = extractNextPossibleText(data, currentSearchStart);
                String possibleTextString = extraction.text; // 获取提取到的字符串

                if (!possibleTextString.isEmpty()) { // 如果提取到了非空字符串
                    // 计算提取字符串的MD5哈希
                    String currentHash = CryptoUtils.calculateMD5(possibleTextString);
                    if (hash.equalsIgnoreCase(currentHash)) { // 如果哈希匹配（不区分大小写）
                        return possibleTextString; // 返回找到的原文
                    }
                }
                // 更新下一个搜索的起始位置，从提取结束位置的下一个字符开始
                currentSearchStart = extraction.nextIndex + 1;
            }
            return null; // 没有找到匹配的原文
        }
    }

    /**
     * SHA1Searcher 是一个 Callable 任务，用于在给定的字节数据块中搜索与目标SHA-1哈希匹配的原文。
     */
    public static class SHA1Searcher implements Callable<String> {
        private final String hash;    // 目标SHA-1哈希值
        private final byte[] data;    // 要搜索的字节数据块

        public SHA1Searcher(String hash, byte[] data) {
            this.hash = hash;
            this.data = data;
        }

        @Override
        public String call() {
            int currentSearchStart = 0; // 当前搜索的起始位置
            while (currentSearchStart < data.length) {
                if (Thread.currentThread().isInterrupted()) return null; // 检查线程是否被中断
                
                // 提取下一个可能的文本字符串及其结束位置
                ExtractedTextResult extraction = extractNextPossibleText(data, currentSearchStart);
                String possibleTextString = extraction.text; // 获取提取到的字符串

                if (!possibleTextString.isEmpty()) { // 如果提取到了非空字符串
                    // 计算提取字符串的SHA-1哈希
                    String currentHash = CryptoUtils.calculateSHA1(possibleTextString);
                    if (hash.equalsIgnoreCase(currentHash)) { // 如果哈希匹配（不区分大小写）
                        return possibleTextString; // 返回找到的原文
                    }
                }
                // 更新下一个搜索的起始位置，从提取结束位置的下一个字符开始
                currentSearchStart = extraction.nextIndex + 1;
            }
            return null; // 没有找到匹配的原文
        }
    }

    /**
     * SHA256Searcher 是一个 Callable 任务，用于在给定的字节数据块中搜索与目标SHA-256哈希匹配的原文。
     */
    public static class SHA256Searcher implements Callable<String> {
        private final String hash;    // 目标SHA-256哈希值
        private final byte[] data;    // 要搜索的字节数据块

        public SHA256Searcher(String hash, byte[] data) {
            this.hash = hash;
            this.data = data;
        }

        @Override
        public String call() {
            int currentSearchStart = 0; // 当前搜索的起始位置
            while (currentSearchStart < data.length) {
                if (Thread.currentThread().isInterrupted()) { // 检查线程是否被中断
                    return null;
                }

                // 提取下一个可能的文本字符串及其结束位置
                ExtractedTextResult extraction = extractNextPossibleText(data, currentSearchStart);
                String possibleTextString = extraction.text; // 获取提取到的字符串
                
                if (!possibleTextString.isEmpty()) { // 如果提取到了非空字符串
                    // 计算提取字符串的SHA-256哈希
                    String currentHash = CryptoUtils.calculateSHA256(possibleTextString);
                    if (hash.equalsIgnoreCase(currentHash)) { // 如果哈希匹配（不区分大小写）
                        return possibleTextString; // 返回找到的原文
                    }
                }
                // 更新下一个搜索的起始位置，从提取结束位置的下一个字符开始
                currentSearchStart = extraction.nextIndex + 1;
            }
            return null; // 没有找到匹配的原文
        }
    }
} 