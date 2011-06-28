/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.easyant.core.ant.listerners;

import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.util.StringUtils;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.util.DateUtils;

/*
 * This listener is created so that it may be used to provide all statistic
 * collection for easyant builds.
 */

/**
 * Build listener to time execution of builds.
 */
public class BuildExecutionTimer implements BuildListener {
    // build start time
    // to be initialized in buildStarted method
    protected long buildStartTime;

    public static class ExecutionResult {
        /**
         * Name of the unit whose execution is timed
         */
        private String unitName;

        /**
         * Time taken to execute the unit
         */
        private long elapsedTime;

        /**
         * Formatted representation of the execution time
         */
        private String formattedElapsedTime;

        /**
         * Execution result
         */
        private int execResult;

        /**
         * Value to be assigned to execResult if failure
         */
        public static int FAILURE_RESULT = 0;

        /**
         * Value to be assigned to execResult if success
         */
        public static int SUCCESS_RESULT = 1;

        public ExecutionResult(String unitName, long elapsedTime,
                int buildResult) {
            this.unitName = unitName;
            this.elapsedTime = elapsedTime;
            this.formattedElapsedTime = DateUtils
                    .formatElapsedTime(elapsedTime);
            this.execResult = buildResult;
        }

        public String getUnitName() {
            return this.unitName;
        }

        public long getElapsedTime() {
            return this.elapsedTime;
        }

        public String getFormattedElapsedTime() {
            return this.formattedElapsedTime;
        }

        public int getResult() {
            return this.execResult;
        }
    }

    /**
     * stops the timer and stores the result as a project reference by the key
     * 'referenceName'
     */
    protected void stopTimer(BuildEvent arg0, String referenceName,
            long startTime) {
        List<ExecutionResult> results = (List<ExecutionResult>) arg0
                .getProject().getReference(referenceName);
        if (results == null) {
            results = new ArrayList<ExecutionResult>();
            arg0.getProject().addReference(referenceName, results);
        }

        ExecutionResult execResult = new ExecutionResult(arg0.getProject()
                .getName(), System.currentTimeMillis() - startTime, arg0
                .getException() == null ? ExecutionResult.SUCCESS_RESULT
                : ExecutionResult.FAILURE_RESULT);

        results.add(execResult);

    }

    public void buildFinished(BuildEvent arg0) {
        stopTimer(arg0, EXECUTION_TIMER_BUILD_RESULTS, buildStartTime);
    }

    public void buildStarted(BuildEvent arg0) {
        buildStartTime = System.currentTimeMillis();
    }

    public void messageLogged(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    public void targetFinished(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    /* following methods may be implemented for more comprehensive stats */
    public void targetStarted(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    public void taskFinished(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    public void taskStarted(BuildEvent arg0) {
        // leaving empty till better use is found
    }

    /**
     * Returns a string containing results of execution timing for display on
     * console in a tabular fashion
     * 
     * @param results
     * @return
     */
    public static String formatExecutionResults(List<ExecutionResult> results) {
        String formattedResults = "";
        int constantSpaces = 10;
        int maxUnitNameLength = 0;
        int maxExecTimeLength = 0;
        for (int i = 0; i < results.size(); i++) {
            ExecutionResult result = results.get(i);
            maxUnitNameLength = result.getUnitName().length() > maxUnitNameLength ? result
                    .getUnitName().length()
                    : maxUnitNameLength;
            maxExecTimeLength = result.getFormattedElapsedTime().length() > maxExecTimeLength ? result
                    .getFormattedElapsedTime().length()
                    : maxExecTimeLength;
        }
        StringBuffer sb = new StringBuffer(
                org.apache.tools.ant.util.StringUtils.LINE_SEP);
        for (int i = 0; i < results.size(); i++) {
            ExecutionResult result = results.get(i);
            String moduleName = result.getUnitName();
            int variableSpaces = maxUnitNameLength - moduleName.length()
                    + constantSpaces;
            sb.append(" * ").append(result.getUnitName()).append(
                    StringUtils.repeat(" ", variableSpaces));
            // keeping both success and failed strings of equal length
            String execResult = result.getResult() == ExecutionResult.SUCCESS_RESULT ? "SUCCESS "
                    : "FAILED  ";
            sb.append(execResult).append("[took ").append(
                    result.getFormattedElapsedTime()).append("]").append(
                    org.apache.tools.ant.util.StringUtils.LINE_SEP);
        }

        formattedResults = sb.toString();
        return formattedResults;
    }

    /** 
     * Reference key against which build execution results will be stored
     */
    public static final String EXECUTION_TIMER_BUILD_RESULTS = "execution.timer.build.results";
}