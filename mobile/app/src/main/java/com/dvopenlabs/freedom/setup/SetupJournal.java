// SPDX-License-Identifier: GPL-3.0-or-later
package com.dvopenlabs.freedom.setup;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import org.json.JSONObject;

/** Fsynced append-before-write journal. A torn final entry is ignored, not replayed. */
final class SetupJournal {
    final File file;
    SetupJournal(File directory){directory.mkdirs();file=new File(directory,"operation.jsonl");}
    synchronized void append(JSONObject record)throws Exception{
        // Validate every complete record before repairing only an incomplete final append.
        last();if(file.isFile()){byte[] prior=java.nio.file.Files.readAllBytes(file.toPath());int end=prior.length;while(end>0&&prior[end-1]!='\n')end--;if(end<prior.length)try(RandomAccessFile repair=new RandomAccessFile(file,"rw")){repair.setLength(end);repair.getFD().sync();}}
        byte[] data=record.toString().getBytes(StandardCharsets.UTF_8);if(data.length>256*1024||file.length()+data.length+10>4*1024*1024)throw new IOException("Recovery journal is full; use desktop Setup for recovery before another installation");CRC32 crc=new CRC32();crc.update(data);byte[] line=(String.format(java.util.Locale.ROOT,"%08x",crc.getValue())+" "+new String(data,StandardCharsets.UTF_8)+"\n").getBytes(StandardCharsets.UTF_8);try(FileOutputStream out=new FileOutputStream(file,true)){out.write(line);out.getFD().sync();}}
    synchronized JSONObject last()throws Exception{if(!file.isFile())return null;if(file.length()>4*1024*1024)throw new IOException("Recovery journal exceeds its limit");byte[] bytes=java.nio.file.Files.readAllBytes(file.toPath());String text=new String(bytes,StandardCharsets.UTF_8);JSONObject last=null;String[] lines=text.split("\n",-1);for(int n=0;n<lines.length-1;n++){String line=lines[n];if(line.length()<10)throw new IOException("Recovery record is damaged");byte[] data=line.substring(9).getBytes(StandardCharsets.UTF_8);CRC32 crc=new CRC32();crc.update(data);if(!line.substring(0,8).equals(String.format(java.util.Locale.ROOT,"%08x",crc.getValue()))||line.charAt(8)!=' ')throw new IOException("Recovery record is damaged");last=new JSONObject(line.substring(9));}return last;}
}
