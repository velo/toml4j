package com.moandjiezana.toml;

import static com.moandjiezana.toml.ValueWriters.WRITERS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * <p>Converts Objects to TOML</p>
 *
 * <p>An input Object can comprise arbitrarily nested combinations of Java primitive types,
 * other {@link Object}s, {@link Map}s, {@link List}s, and Arrays. {@link Object}s and {@link Map}s
 * are output to TOML tables, and {@link List}s and Array to TOML arrays.</p>
 *
 * <p>Example usage:</p>
 * <pre><code>
 * class AClass {
 *   int anInt = 1;
 *   int[] anArray = { 2, 3 };
 * }
 *
 * String tomlString = new TomlWriter().write(new AClass());
 * </code></pre>
 */
public class TomlWriter {
  
  public static class Builder {
    private int keyIndentation;
    private int tableIndentation;
    private int arrayDelimiterPadding = 0;
    private TimeZone timeZone = TimeZone.getTimeZone("UTC");
    
    public TomlWriter.Builder indentValuesBy(int spaces) {
      this.keyIndentation = spaces;
      
      return this;
    }

    public TomlWriter.Builder indentTablesBy(int spaces) {
      this.tableIndentation = spaces;
      
      return this;
    }
    
    public TomlWriter.Builder timeZone(TimeZone timeZone) {
      this.timeZone = timeZone;
      
      return this;
    }
    
    /**
     * @param spaces number of spaces to put between opening square bracket and first item and between closing square bracket and last item
     * @return this TomlWriter.Builder instance
     */
    public TomlWriter.Builder padArrayDelimitersBy(int spaces) {
      this.arrayDelimiterPadding = spaces;
      
      return this;
    }
    
    public TomlWriter build() {
      return new TomlWriter(keyIndentation, tableIndentation, arrayDelimiterPadding, timeZone);
    }
  }

  private final WriterIndentationPolicy indentationPolicy;
  private TimeZone timeZone;

  /**
   * Creates a TomlWriter instance.
   */
  public TomlWriter() {
    this(0, 0, 0, TimeZone.getTimeZone("UTC"));
  }
  
  private TomlWriter(int keyIndentation, int tableIndentation, int arrayDelimiterPadding, TimeZone timeZone) {
    this.indentationPolicy = new WriterIndentationPolicy(keyIndentation, tableIndentation, arrayDelimiterPadding);
    this.timeZone = timeZone;
  }

  /**
   * Write an Object into TOML String.
   *
   * @param from the object to be written
   * @return a string containing the TOML representation of the given Object
   */
  public String write(Object from) {
    try {
      StringWriter output = new StringWriter();
      write(from, output);
      
      return output.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write an Object in TOML to a {@link OutputStream}.
   *
   * @param from the object to be written
   * @param target the OutputStream to which the TOML will be written. The stream is not closed after being written to.
   * @throws IOException if target.write() fails
   */
  public void write(Object from, OutputStream target) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(target);
    write(from, writer);
    writer.flush();
  }

  /**
   * Write an Object in TOML to a {@link File}.
   *
   * @param from the object to be written
   * @param target the File to which the TOML will be written
   * @throws IOException if any file operations fail
   */
  public void write(Object from, File target) throws IOException {
    FileWriter writer = new FileWriter(target);
    try {
      write(from, writer);
    } finally {
      writer.close();
    }
  }

  /**
   * Write an Object in TOML to a {@link Writer}.
   *
   * @param from the object to be written
   * @param target the Writer to which TOML will be written. The Writer is not closed.
   * @throws IOException if target.write() fails
   */
  public void write(Object from, Writer target) throws IOException {
    WriterContext context = new WriterContext(indentationPolicy, timeZone, target);
    WRITERS.write(from, context);
  }

  WriterIndentationPolicy getIndentationPolicy() {
    return indentationPolicy;
  }
}