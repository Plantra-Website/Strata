package com.strata.core;

import java.util.List;
import java.util.Map;

public class JsonTest {
   static int failures = 0;

   static void check(boolean cond, String msg) {
      if (!cond) { failures++; System.out.println("FAIL: " + msg); }
   }

   static void checkThrows(String text, String msg) {
      try {
         Json.parse(text);
      } catch (RuntimeException e) {
         return;
      }
      failures++;
      System.out.println("FAIL (no throw): " + msg);
   }

   @SuppressWarnings("unchecked")
   public static void main(String[] args) {
      check(Json.parse(" 123 ").equals(123.0D), "int");
      check(Json.parse("-2.5e3").equals(-2500.0D), "float exp");
      check(Json.parse("true").equals(Boolean.TRUE), "true");
      check(Json.parse("false").equals(Boolean.FALSE), "false");
      check(Json.parse("null") == null, "null");
      check(Json.parse("\"a\\\"b\\n\\u0041\"").equals("a\"b\nA"), "escapes");
      check(Json.parse("[]").equals(List.of()), "empty array");
      check(Json.parse("{}").equals(Map.of()), "empty object");
      Object root = Json.parse("{\"replace\": false, \"values\": [11, 22]}");
      check(root instanceof Map, "tag root object");
      Map<String, Object> tag = (Map<String, Object>)root;
      check(tag.get("replace").equals(Boolean.FALSE), "tag replace flag");
      check(tag.get("values").equals(List.of(11.0D, 22.0D)), "tag values");
      Map<String, Object> viaHelper = Json.parseObject("{\"values\": []}");
      check(viaHelper.containsKey("values"), "parseObject helper");
      checkThrows("", "empty");
      checkThrows("{", "unclosed object");
      checkThrows("[1,", "unclosed array");
      checkThrows("{\"a\" 1}", "missing colon");
      checkThrows("\"\\x\"", "bad escape");
      checkThrows("01", "leading zero");
      checkThrows("1. ", "bare decimal point");
      checkThrows("[1] trailing", "trailing content");
      checkThrows("nul", "bad literal");
      checkThrows("{\"a\": 1, \"a\": 2}", "duplicate key");
      checkThrows("\"tab\there\"", "unescaped control");
      checkThrows("\"\\uD83D\"", "unpaired high surrogate");
      checkThrows("\"\\uDE00\"", "lone low surrogate");
      check(Json.parse("\"\\uD83D\\uDE00\"").equals("\uD83D\uDE00"), "surrogate pair");
      check(Json.parse("true ").equals(Boolean.TRUE), "literal delimited by space");
      try {
         Json.parseObject("[1]");
         failures++;
         System.out.println("FAIL (no throw): non-object root");
      } catch (RuntimeException e) {
      }

      if (failures == 0) System.out.println("JSON PASS");
      else { System.out.println(failures + " FAILURES"); System.exit(1); }
   }
}

