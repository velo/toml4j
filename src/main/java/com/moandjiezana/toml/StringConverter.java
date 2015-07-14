package com.moandjiezana.toml;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StringConverter implements ValueConverter, ValueWriter {
  
  static final StringConverter STRING_PARSER = new StringConverter();
  private static final Pattern UNICODE_REGEX = Pattern.compile("\\\\[uU](.{4})");

  static private final String[] specialCharacterEscapes = new String[93];

  static {
    specialCharacterEscapes[0x08] = "\\b";
    specialCharacterEscapes[0x09] = "\\t";
    specialCharacterEscapes[0x0A] = "\\n";
    specialCharacterEscapes[0x0C] = "\\f";
    specialCharacterEscapes[0x0D] = "\\r";
    specialCharacterEscapes[0x22] = "\\\"";
    specialCharacterEscapes[0x5C] = "\\\\";
  }

  @Override
  public boolean canConvert(String s) {
    return s.startsWith("\"");
  }

  @Override
  public Object convert(String s, AtomicInteger index, Context context) {
    int startIndex = index.incrementAndGet();
    int endIndex = -1;

    for (int i = index.get(); i < s.length(); i = index.incrementAndGet()) {
      char ch = s.charAt(i);
      if (ch == '"' && s.charAt(i - 1) != '\\') {
        endIndex = i;
        break;
      }
    }

    if (endIndex == -1) {
      Results.Errors errors = new Results.Errors();
      errors.unterminated(context.identifier.getName(), s.substring(startIndex - 1), context.line.get());
      return errors;
    }
    
    String raw = s.substring(startIndex, endIndex);
    s = replaceUnicodeCharacters(raw);
    s = replaceSpecialCharacters(s);
    
    if (s == null) {
      Results.Errors errors = new Results.Errors();
      errors.invalidValue(context.identifier.getName(), raw, context.line.get());
      return errors;
    }

    return s;
  }

  String replaceUnicodeCharacters(String value) {
    Matcher unicodeMatcher = UNICODE_REGEX.matcher(value);

    while (unicodeMatcher.find()) {
      value = value.replace(unicodeMatcher.group(), new String(Character.toChars(Integer.parseInt(unicodeMatcher.group(1), 16))));
    }
    return value;
  }

  String replaceSpecialCharacters(String s) {
    for (int i = 0; i < s.length() - 1; i++) {
      char ch = s.charAt(i);
      char next = s.charAt(i + 1);

      if (ch == '\\' && next == '\\') {
        i++;
      } else if (ch == '\\' && !(next == 'b' || next == 'f' || next == 'n' || next == 't' || next == 'r' || next == '"' || next == '\\')) {
        return null;
      }
    }

    return s.replace("\\n", "\n")
      .replace("\\\"", "\"")
      .replace("\\t", "\t")
      .replace("\\r", "\r")
      .replace("\\\\", "\\")
      .replace("\\/", "/")
      .replace("\\b", "\b")
      .replace("\\f", "\f");
  }

  @Override
  public boolean canWrite(Object value) {
    return value instanceof String || value instanceof Character || value instanceof URL || value instanceof URI || value instanceof Enum;
  }

  @Override
  public void write(Object value, WriterContext context) {
    context.write('"');
    escapeUnicode(value.toString(), context);
    context.write('"');
  }

  @Override
  public boolean isPrimitiveType() {
    return true;
  }

  private void escapeUnicode(String in, WriterContext context) {
    for (int i = 0; i < in.length(); i++) {
      int codePoint = in.codePointAt(i);
      if (codePoint < specialCharacterEscapes.length && specialCharacterEscapes[codePoint] != null) {
        context.write(specialCharacterEscapes[codePoint]);
      } else {
        context.write(in.charAt(i));
      }
    }
  }

  private StringConverter() {}

  @Override
  public String toString() {
    return "string";
  }
}
