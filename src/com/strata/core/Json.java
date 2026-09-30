package com.strata.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
   private Json() {
   }

   public static Object parse(String text) {
      Parser p = new Parser(text);
      Object v = p.value();
      p.ws();
      if (!p.end()) {
         throw p.fail("trailing content");
      }
      return v;
   }

   @SuppressWarnings("unchecked")
   public static Map<String, Object> parseObject(String text) {
      Object v = parse(text);
      if (!(v instanceof Map)) {
         throw new RuntimeException("json root is not an object");
      }
      return (Map<String, Object>) v;
   }

   private static final class Parser {
      private final String s;
      private int pos;

      Parser(String s) {
         this.s = s;
      }

      boolean end() {
         return this.pos >= this.s.length();
      }

      void ws() {
         while (!this.end()) {
            char c = this.s.charAt(this.pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
               this.pos++;
            } else {
               return;
            }
         }
      }

      RuntimeException fail(String what) {
         return new RuntimeException("json " + what + " at offset " + this.pos);
      }

      Object value() {
         this.ws();
         if (this.end()) {
            throw this.fail("unexpected end");
         }
         char c = this.s.charAt(this.pos);
         if (c == '{') {
            return this.object();
         }
         if (c == '[') {
            return this.array();
         }
         if (c == '"') {
            return this.string();
         }
         if (c == 't' || c == 'f' || c == 'n') {
            return this.literal();
         }
         if (c == '-' || isDigit(c)) {
            return this.number();
         }
         throw this.fail("unexpected token '" + c + "'");
      }

      Map<String, Object> object() {
         Map<String, Object> map = new LinkedHashMap<>();
         this.pos++; 
         this.ws();
         if (!this.end() && this.s.charAt(this.pos) == '}') {
            this.pos++;
            return map;
         }
         while (true) {
            this.ws();
            if (this.end() || this.s.charAt(this.pos) != '"') {
               throw this.fail("expected string key");
            }
            String key = this.string();
            if (map.containsKey(key)) {
               throw this.fail("duplicate key '" + key + "'");
            }
            this.ws();
            if (this.end() || this.s.charAt(this.pos) != ':') {
               throw this.fail("expected ':'");
            }
            this.pos++;
            map.put(key, this.value());
            this.ws();
            if (this.end()) {
               throw this.fail("unterminated object");
            }
            char c = this.s.charAt(this.pos++);
            if (c == '}') {
               return map;
            }
            if (c != ',') {
               throw this.fail("expected ',' or '}'");
            }
         }
      }

      List<Object> array() {
         List<Object> list = new ArrayList<>();
         this.pos++; 
         this.ws();
         if (!this.end() && this.s.charAt(this.pos) == ']') {
            this.pos++;
            return list;
         }
         while (true) {
            list.add(this.value());
            this.ws();
            if (this.end()) {
               throw this.fail("unterminated array");
            }
            char c = this.s.charAt(this.pos++);
            if (c == ']') {
               return list;
            }
            if (c != ',') {
               throw this.fail("expected ',' or ']'");
            }
         }
      }

      String string() {
         StringBuilder out = new StringBuilder();
         this.pos++; 
         while (true) {
            if (this.end()) {
               throw this.fail("unterminated string");
            }
            char c = this.s.charAt(this.pos++);
            if (c == '"') {
               return out.toString();
            }
            if (c < 0x20) {
               throw this.fail("unescaped control character in string");
            }
            if (c != '\\') {
               out.append(c);
               continue;
            }
            if (this.end()) {
               throw this.fail("unterminated escape");
            }
            char e = this.s.charAt(this.pos++);
            if (e == '"') {
               out.append('"');
            } else if (e == '\\') {
               out.append('\\');
            } else if (e == '/') {
               out.append('/');
            } else if (e == 'b') {
               out.append('\b');
            } else if (e == 'f') {
               out.append('\f');
            } else if (e == 'n') {
               out.append('\n');
            } else if (e == 'r') {
               out.append('\r');
            } else if (e == 't') {
               out.append('\t');
            } else if (e == 'u') {
               char codeUnit = this.parseHex4();
               if (Character.isHighSurrogate(codeUnit)) {
                  if (this.pos + 6 > this.s.length() ||
                      this.s.charAt(this.pos) != '\\' ||
                      this.s.charAt(this.pos + 1) != 'u') {
                     throw this.fail("unpaired high surrogate");
                  }
                  this.pos += 2;
                  char low = this.parseHex4();
                  if (!Character.isLowSurrogate(low)) {
                     throw this.fail("expected low surrogate after high surrogate");
                  }
                  out.append(codeUnit).append(low);
               } else if (Character.isLowSurrogate(codeUnit)) {
                  throw this.fail("unexpected low surrogate without preceding high surrogate");
               } else {
                  out.append(codeUnit);
               }
            } else {
               throw this.fail("bad escape '\\" + e + "'");
            }
         }
      }

      private char parseHex4() {
         if (this.pos + 4 > this.s.length()) {
            throw this.fail("bad \\u escape");
         }
         try {
            int code = Integer.parseInt(this.s.substring(this.pos, this.pos + 4), 16);
            this.pos += 4;
            return (char) code;
         } catch (NumberFormatException ex) {
            throw this.fail("bad \\u escape");
         }
      }

      Object literal() {
         if (this.matchLiteral("true")) {
            return Boolean.TRUE;
         }
         if (this.matchLiteral("false")) {
            return Boolean.FALSE;
         }
         if (this.matchLiteral("null")) {
            return null;
         }
         throw this.fail("bad literal");
      }

      private boolean matchLiteral(String lit) {
         if (!this.s.startsWith(lit, this.pos)) {
            return false;
         }
         int nextPos = this.pos + lit.length();
         if (nextPos < this.s.length()) {
            char nextChar = this.s.charAt(nextPos);
            if (isLiteralChar(nextChar)) {
               return false;
            }
         }
         this.pos = nextPos;
         return true;
      }

      Double number() {
         int start = this.pos;
         if (!this.end() && this.s.charAt(this.pos) == '-') {
            this.pos++;
         }
         if (this.end()) {
            throw this.fail("bad number");
         }
         if (this.s.charAt(this.pos) == '0') {
            this.pos++;
         } else if (this.s.charAt(this.pos) >= '1' && this.s.charAt(this.pos) <= '9') {
            while (!this.end() && isDigit(this.s.charAt(this.pos))) {
               this.pos++;
            }
         } else {
            throw this.fail("bad number");
         }
         if (!this.end() && this.s.charAt(this.pos) == '.') {
            this.pos++;
            if (this.end() || !isDigit(this.s.charAt(this.pos))) {
               throw this.fail("bad number");
            }
            while (!this.end() && isDigit(this.s.charAt(this.pos))) {
               this.pos++;
            }
         }
         if (!this.end() && (this.s.charAt(this.pos) == 'e' || this.s.charAt(this.pos) == 'E')) {
            this.pos++;
            if (!this.end() && (this.s.charAt(this.pos) == '+' || this.s.charAt(this.pos) == '-')) {
               this.pos++;
            }
            if (this.end() || !isDigit(this.s.charAt(this.pos))) {
               throw this.fail("bad number");
            }
            while (!this.end() && isDigit(this.s.charAt(this.pos))) {
               this.pos++;
            }
         }
         try {
            return Double.valueOf(this.s.substring(start, this.pos));
         } catch (NumberFormatException ex) {
            throw this.fail("bad number");
         }
      }

      private static boolean isDigit(char c) {
         return c >= '0' && c <= '9';
      }

      private static boolean isLiteralChar(char c) {
         return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || isDigit(c) || c == '_' || c == '$';
      }
   }
}

