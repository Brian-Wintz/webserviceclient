package com.bkw.webserviceclient;

import java.io.*;
import java.net.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

public class WebServiceClient implements Runnable {
  protected static String content = "";
  protected static String host = "coli45.qad.com";
  protected static int port = 4521;
  protected static Hashtable<String,String> ht = new Hashtable<String,String>();
  protected static String todayString = "";
  protected static NumberFormat nf2 = NumberFormat.getInstance();
  protected static NumberFormat nf3 = NumberFormat.getInstance();
  protected static SimpleDateFormat sdf;

  protected static long cumTime=0;
  protected static long cumCount=0;
  protected int thread;
  protected int iteration;

  static {
    nf3.setMinimumIntegerDigits(2);
    nf2.setMinimumIntegerDigits(2);
    sdf = new SimpleDateFormat("yyyy-MM-dd");
    todayString = sdf.format(new Date());
    sdf = new SimpleDateFormat("HH:mm:ss");
  }

  /**
   * @param args
   *   args[0]=host
   *   args[1]=port
   *   args[2]=local file name
   *   args[3]=number of threads
   *   args[4]=number of iterations
   */
  public static void main(String[] args) {
    ht.put("SO","SO1");
    ht.put("PO","PO1");
    try {
      if(args.length > 5) ht.put("SO", args[5]);
      if(args.length > 6) ht.put("PO", args[6]);
      host = args[0];
      port = Integer.parseInt(args[1]);
      // Read file (args[1])
      BufferedReader in = new BufferedReader(new FileReader(args[2]));
      while (in.ready()) {
        String line = in.readLine();
        content += (content.length() == 0 ? "" : "\r\n") + line;
      }
      in.close();

      // Start args[3] number of threads
      int numThreads = Integer.parseInt(args[3]);
      Thread threads[] = new Thread[numThreads];
      for(int i=0; i<threads.length; ++i) {
        WebServiceClient wsc = new WebServiceClient(i);
        threads[i] = new Thread(wsc);
      }

      // Run number of threads args[4] times
//System.out.println("START:"+System.currentTimeMillis());
      int iterations = Integer.parseInt(args[4]);
/*
System.out.println("NEW:"+Thread.State.NEW+
    "\nRUNNABLE:"+Thread.State.RUNNABLE+
    "\nBLOCKED:"+Thread.State.BLOCKED+
    "\nWAITING:"+Thread.State.WAITING+
    "\nTIMED_WAITING:"+Thread.State.TIMED_WAITING+
    "\nTERMINATED:"+Thread.State.TERMINATED);
*/
      for(int i=0; i<iterations; ++i) {
        for(int j=0; j<numThreads; ++j) {
//System.out.println(i+":THREAD["+j+"]:"+threads[j].getState());
          while(threads[j].isAlive()) {
/*
          while(threads[j].getState() != Thread.State.NEW &&
                threads[j].getState() != Thread.State.TERMINATED) {
*/
            Thread.currentThread().sleep(500);
//System.out.println("  THREAD["+j+"]:"+threads[j].getState());

          }
//System.out.println("##:"+i+":THREAD["+j+"]:"+threads[j].getState());
          WebServiceClient wsc = new WebServiceClient(j);
          wsc.setIteration(i);
          threads[j] = new Thread(wsc);
//System.out.println("XX:"+i+":THREAD["+j+"]:"+threads[j].getState());
          threads[j].start();
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Cumulative Time:"+cumTime+" Count:"+cumCount);
  }

  public WebServiceClient(int thread){
    this.thread = thread;
  }

  public void setIteration(int iteration) {
    this.iteration = iteration;
  }

  public void run() {
    try {
      String data = replace(content,this.iteration,this.thread);
      if(iteration == 0 && thread == 0)
        System.out.println("data:"+data);
      System.out.println("START:"+Thread.currentThread()+":"+thread+":"+iteration+":"+System.currentTimeMillis());
      long start = System.currentTimeMillis();
      Socket socket = new Socket(host,port);
      InputStream in = socket.getInputStream();
      OutputStream out = socket.getOutputStream();
      out.write(data.getBytes());
      System.out.println(Thread.currentThread()+":SENT REQUEST");

      long end1 = System.currentTimeMillis();

      String result = "";
      int c;
      while((c=in.read()) != -1){
        result += (char)c;
      }
      long end2 = System.currentTimeMillis();
      cumTime += (end2-start);
      ++cumCount;
      System.out.println(Thread.currentThread()+":RECEIVED RESPONSE:"+(end1-start)+":"+(end2-start)+result);
      System.out.println("THREAD:"+thread+" ITERATION:"+iteration+":"+result);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private static String replaceLiteral(String in,int start) {
    String result = in;
    int end = in.indexOf("}",start);
    if(end >=0 ) {
      String literal = in.substring(start+9,end);
      String value = ht.get(literal);
      if(value == null) {
        System.err.println("Missing value for referenced '"+literal+"' value, using blank");
        value = "";
      }
      if(value != null) {
        result = in.substring(0,start) + value + in.substring(end+1);
      }
    }
    return result;
  }

  private static String replaceThread(String in,int start,int thread) {
    String result = in;
    int end = in.indexOf("}",start);
    if(end >=0 ) {
      String value = nf2.format(thread);
      result = in.substring(0,start) + value + in.substring(end+1);
    }
    return result;
  }

  private static String replaceCount(String in,int start,int count) {
    String result = in;
    int end = in.indexOf("}",start);
    if(end >=0 ) {
      String value = nf3.format(count);
      result = in.substring(0,start) + value + in.substring(end+1);
    }
    return result;
  }

  private static String replaceDate(String in,int start) {
    String result = in;
    int end = in.indexOf("}",start);
    if(end >=0 ) {
      result = in.substring(0,start) + todayString + in.substring(end+1);
    }
    return result;
  }

  private static String replaceTime(String in,int start) {
    String result = in;
    int end = in.indexOf("}",start);
    if(end >=0 ) {
      String value = sdf.format(new Date());
      result = in.substring(0,start) + value + in.substring(end+1);
    }
    return result;
  }

  private static String addHeader(String in) {
    int size = in.length();
    String result = "POST /reqstreplyout HTTP/1.0\r\nCONTENT-TYPE: text/xml\r\nCONTENT-LENGTH: "+size+"\r\n\r\n"+in;
    return result;
  }

  public static String replace(String in,int count,int thread){
    // Handle {LITERAL:{name}} tag
    int i = in.indexOf("{LITERAL:");
    while(i >= 0) {
      in = replaceLiteral(in,i);
      i = in.indexOf("{LITERAL:");
    }
    // Handle {COUNT} tag
    i = in.indexOf("{COUNT}");
    while(i >= 0) {
      in = replaceCount(in,i,count);
      i = in.indexOf("{COUNT}");
    }
    // Handle {THREAD} tag
    i = in.indexOf("{THREAD}");
    while(i >= 0) {
      in = replaceThread(in,i,thread);
      i = in.indexOf("{THREAD}");
    }
    // Handle {DATE} tag
    i = in.indexOf("{DATE}");
    while(i >= 0) {
      in = replaceDate(in,i);
      i = in.indexOf("{DATE}");
    }
    // Handle {TIME} tag
    i = in.indexOf("{TIME}");
    while(i >= 0) {
      in = replaceTime(in,i);
      i = in.indexOf("{TIME}");
    }
    // Add HTTP header
    in = addHeader(in);

    return in;
  }
}
