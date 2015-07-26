package org.javaseis.tool;

import java.util.HashMap;
import java.util.Map;

import org.javaseis.grid.GridDefinition;
import org.javaseis.services.ParameterService;
import org.javaseis.volume.ISeismicVolume;

import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.parallel.ParallelMap;
import beta.javaseis.parallel.SharedMap;
import beta.javaseis.parallel.UniprocessorContext;

/**
 * Container class for consolidating things developers would like to know about
 * the data and the tool environment
 *
 * @author Chuck Mosher for JavaSeis.org
 *
 */
public class ToolContext {

// Static fields for common parameters
  public static String HAS_OUTPUT = "hasOutput";
  public static String OUTPUT_FILE_PATH = "outputFilePath";
  public static String OUTPUT_FILE_SYSTEM = "outputFileSystem";
  public static String OUTPUT_FILE_MODE = "outputFileMode";
  public static String OUTPUT_FILE_CREATE = "create";
  public static String OUTPUT_GRID = "outputGrid";
  public static String HAS_INPUT = "hasInput";
  public static String INPUT_FILE_PATH = "inputFilePath";
  public static String INPUT_FILE_SYSTEM = "inputFileSystem";
  public static String INPUT_GRID = "inputGrid";
  public static String TASK_COUNT = "taskCount";
  public static String TOOL_CLASS = "toolClass";
  
  /** Visibility of stored objects */
  public enum Visibility {
    /** Visible only within the task for this tool */
    TOOL_LOCAL,
    /** Visible to all tasks for this tool */
    TOOL_GLOBAL,
    /** Visible to all tools within a task */
    FLOW_LOCAL,
    /** Visible to all tools and all tasks */
    FLOW_GLOBAL,
  }
  
  public ParameterService parms;
  public IParallelContext pc;
  public SharedMap toolGlobal;
  public SharedMap flowGlobal;
  public Map<String,Object> toolLocal;
  public Map<String,Object> flowLocal;


  public ToolContext() {
    parms = new ParameterService((String[])null);
    pc = new UniprocessorContext();
    flowGlobal = new SharedMap();
    toolGlobal = new SharedMap();
    flowLocal = new HashMap<String,Object>();
    toolLocal = new HashMap<String,Object>();
  }
  
  public ToolContext( ParameterService parameterService ) {
    this();
    parms = parameterService;
  }
  
  public ToolContext( ToolContext sourceContext ) {
    parms = sourceContext.parms;
    parms.lock();
    pc = sourceContext.pc;
    flowGlobal = new SharedMap();
    flowGlobal.putAll(sourceContext.flowGlobal.getMap());
    flowGlobal.merge(pc);
    flowLocal = new HashMap<String,Object>( sourceContext.flowLocal );
    toolGlobal = new SharedMap();
    toolGlobal.putAll(sourceContext.toolGlobal.getMap());
    toolGlobal.merge(pc);
    toolLocal = new HashMap<String,Object>( sourceContext.toolLocal );
  }

  public ToolContext(ParameterService parameterService, IParallelContext parallelContext, SharedMap sharedMap, Map<String,Object> localMap ) {
    parms = parameterService;
    parms.lock();
    pc = parallelContext;
    flowGlobal = sharedMap;
    flowLocal = localMap;
    toolGlobal = new SharedMap();
    toolLocal = new HashMap<String,Object>();
  }
  
  public void setParallelContext( IParallelContext parallelContext ) {
    pc = parallelContext;
    syncGlobalObjects();
  }
  
  public IParallelContext getParallelContext() {
    return pc;
  }
  
  public String getParameter( String key ) {
    return parms.getParameter(key);
  }
  
  public void putFlowLocal( String key, Object value ) {
    flowLocal.put(key, value);
  }
  
  public Object getFlowLocal( String key ) {
    return flowLocal.get(key);
  }
  
  public void putFlowGlobal( String key, Object value ) {
    flowGlobal.put(key, value);
  }
  
  public Object getFlowGlobal( String key ) {
    return flowGlobal.get(key);
  }
  
  public void putToolLocal( String key, Object value ) {
    toolLocal.put(key, value);
  }
  
  public Object getToolLocal( String key ) {
    return toolLocal.get(key);
  }
  
  public void putToolGlobal( String key, Object value ) {
    toolGlobal.put(key, value);
  }
  
  public Object getToolGlobal( String key ) {
    return toolGlobal.get(key);
  }
  
  public void putObject( String key, Object value, Visibility vis) {
    switch( vis ) {
    case FLOW_GLOBAL:
      flowGlobal.put(key, value);
      break;
    case FLOW_LOCAL:
      flowLocal.put(key, value);
      break;
    case TOOL_GLOBAL:
      toolGlobal.put(key, value);
      break;
    case TOOL_LOCAL:
      toolLocal.put(key, value);
      break;
    }
  }
  
  public void mergeFlowMaps( ToolContext sourceContext ) {
    flowGlobal.putAll(sourceContext.flowGlobal.getMap());
    flowGlobal.merge(pc);
    flowLocal.putAll(sourceContext.flowLocal);
  }
  
  public void syncGlobalObjects() {
    flowGlobal.merge(pc);
    toolGlobal.merge(pc);
  }
  
  public static ToolContext copy( ToolContext sourceContext ) {
    ToolContext toolContext = new ToolContext(sourceContext.parms);
    sourceContext.parms.lock();
    toolContext.flowGlobal = new SharedMap();
    toolContext.flowGlobal.putAll(sourceContext.flowGlobal.getMap());
    toolContext.flowGlobal.merge(toolContext.pc);
    toolContext.flowLocal = new HashMap<String,Object>( sourceContext.flowLocal );
    toolContext.toolGlobal = new SharedMap();
    toolContext.toolGlobal.putAll(sourceContext.toolGlobal.getMap());
    toolContext.toolGlobal.merge(toolContext.pc);
    toolContext.toolLocal = new HashMap<String,Object>( sourceContext.toolLocal );
    return toolContext;
  }
}