// https://searchcode.com/api/result/17650706/

/*
 * Copyright (c) 2007 BUSINESS OBJECTS SOFTWARE LIMITED
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *  
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *  
 *     * Neither the name of Business Objects nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */


/*
 * WorkspaceSourceMetricsManager.java
 * Creation date: (Apr 14, 2005)
 * By: Jawright
 */
package org.openquark.cal.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openquark.cal.compiler.ClassInstanceIdentifier.UniversalRecordInstance;
import org.openquark.cal.compiler.SearchResult.Precise;
import org.openquark.cal.compiler.SearchResult.Frequency.Type;
import org.openquark.cal.compiler.SourceIdentifier.Category;
import org.openquark.cal.compiler.SourceMetricFinder.LintWarning;
import org.openquark.cal.compiler.SourceMetricFinder.SearchType;
import org.openquark.cal.compiler.SourceModel.FunctionTypeDeclaration;
import org.openquark.cal.compiler.SourceModel.InstanceDefn;
import org.openquark.cal.compiler.SourceModel.SourceElement;
import org.openquark.cal.compiler.SourceModel.TopLevelSourceElement;
import org.openquark.cal.compiler.SourceModel.InstanceDefn.InstanceTypeCons;
import org.openquark.cal.compiler.SourceModel.InstanceDefn.InstanceTypeCons.TypeCons;
import org.openquark.cal.compiler.SourceModel.TypeClassDefn.ClassMethodDefn;
import org.openquark.cal.compiler.SourceModel.TypeConstructorDefn.AlgebraicType.DataConsDefn;
import org.openquark.cal.filter.AcceptAllModulesFilter;
import org.openquark.cal.filter.AcceptAllQualifiedNamesFilter;
import org.openquark.cal.filter.ModuleFilter;
import org.openquark.cal.filter.QualifiedNameFilter;
import org.openquark.cal.module.Cal.Core.CAL_Prelude;
import org.openquark.util.Pair;
import org.openquark.util.WildcardPatternMatcher;


/**
 * This internal class encapsulates aggregated source metrics for a set of modules.
 * It should be instantiated only by the CALWorkspace class.  Other clients should use
 * the WorkspaceSourceMetrics interface instead. 
 * 
 * This class aggregates the module-level source metric information held in modules' 
 * ModuleTypeInfo objects.
 * 
 * It is not safe to concurrently modify this class.
 *  
 * Creation date: (Apr 14, 2005)
 * @author Jawright
 */
public final class SourceMetricsManager implements SourceMetrics {

    /** Map from gem name to number of times the gem is referred
     * to in the function bodies of functions defined in modules contained by the containing
     * workspace.
     */
    private final Map<QualifiedName, Integer> gemToReferenceFrequencyMap = new HashMap<QualifiedName, Integer>();
    
    /** The containing workspace. */
    private final ModuleContainer moduleContainer;

    /**
     * Used to provide feedback on the progress of the search.
     * 
     * @author GMcClement
     *
     */
    public interface IProgressMonitor {
        /**
         * Called at the start of the search to provide the number of modules to be searched.
         * @param numberOfModules The number of modules that are going to be searched.
         */
        public void numberOfModules(int numberOfModules);
        /**
         * Called just before the given module is searched.
         * @param module The name of the next module to be searched.
         */
        public void startModule(ModuleName module);
        
        /**
         * Called when the search is completed.
         */
        public void done();
        
        /**
         * Check the current activity was cancelled.
         * @return the flag indicating if the activity should be cancelled.
         */
        public boolean isCancelled();

        /**
         * Called when more results from the search are available.
         * @param moreResults The additional search results that have been found.
         */
        public void moreResults(Collection<? extends SearchResult> moreResults);
    }
    
    /** Not intended to be used outside of the CALWorkspace class */
    public SourceMetricsManager(ModuleContainer moduleContainer) {
        this.moduleContainer = moduleContainer;
    }

    /**
     * Add in the metrics for the specified module to the aggregate collection.
     * This method is only intended to be called from CALWorkspace.
     * @param moduleTypeInfo ModuleTypeInfo of module to add to the metrics 
     */
    public synchronized void addModuleMetrics(ModuleTypeInfo moduleTypeInfo) {

        // If there are no metrics being kept, then there's nothing for us to do
        if (moduleTypeInfo.getModuleSourceMetrics() == null) {
            return;
        }
        
        Map<QualifiedName, Integer> updateMap = moduleTypeInfo.getModuleSourceMetrics().getGemToLocalReferenceFrequencyMap();
        
        // Add reference-frequency metrics
        addFrequencyMetrics(updateMap, gemToReferenceFrequencyMap);
    }
    
    /**
     * Remove the metrics for the specified module from the aggregate collection.
     * This method is only intended to be called from CALWorkspace.
     * @param moduleTypeInfo ModuleTypeInfo of module to add to the metrics 
     */
    public synchronized void removeModuleMetrics(ModuleTypeInfo moduleTypeInfo) {

        // If there are no metrics being kept, then there's nothing for us to do
        if (moduleTypeInfo.getModuleSourceMetrics() == null) {
            return;
        }
        
        Map<QualifiedName, Integer> updateMap = moduleTypeInfo.getModuleSourceMetrics().getGemToLocalReferenceFrequencyMap();

        // Remove reference-frequency metrics
        removeFrequencyMetrics(updateMap, gemToReferenceFrequencyMap);
    }

    /**
     * Aggregate the frequency data in sourceMap into destinationMap.
     * If (K,V) is in sourceMap but no mapping for K exists in destinationMap, then
     * (K,V) is added to destinationMap.  If (K,V1) is in sourceMap and (K,V2) is
     * in destinationMap, then destinationMap is updated to contain (K, V1+V2).  
     * @param sourceMap Map to add to destinationMap
     * @param destinationMap Map to update
     */
    private static <T> void addFrequencyMetrics(Map<T, Integer> sourceMap, Map<T, Integer> destinationMap) {
        for (final Map.Entry<T, Integer> entry : sourceMap.entrySet()) {
            Integer newValue = entry.getValue();
            addFrequencyEntry(entry.getKey(), newValue.intValue(), destinationMap);
        }
    }
    
    /**
     * Adds (key, frequency) to destinationMap if destinationMap does not already contain
     * a value for key.  If destinationMap does contain a value for key, then it is replaced
     * by (key, frequency + origValue).
     * @param key Key to update entry for
     * @param frequency Value to add to the entry for key
     * @param destinationMap Map to add the entry to
     */
    private static <T> void addFrequencyEntry(T key, int frequency, Map<T, Integer> destinationMap) {
        Integer currentValue = destinationMap.get(key);
        
        if (currentValue != null) {
            destinationMap.put(key, Integer.valueOf(currentValue.intValue() + frequency));
        } else {
            destinationMap.put(key, Integer.valueOf(frequency));
        }
    }
    
    /**
     * Remove the frequency data in removalItems from the aggregate totals
     * in targetMap.  
     * @param removalItems Map of values to remove from the totals in targetMap
     * @param targetMap Map to update
     */
    private static <T> void removeFrequencyMetrics(Map<T, Integer> removalItems, Map<T, Integer> targetMap) {
        for (final Map.Entry<T, Integer> entry : removalItems.entrySet()) {
            T key = entry.getKey();
            Integer removalValue = entry.getValue();
            Integer currentValue = targetMap.get(key);
            
            if (currentValue != null) {
                targetMap.put(key, Integer.valueOf(currentValue.intValue() - removalValue.intValue()));
                if (currentValue.intValue() - removalValue.intValue() < 0) {
                    throw new IllegalStateException("Module-removal operation on WorkspaceSourceMetricsManager caused a negative frequency for "+key);
                }
            } else {
                throw new IllegalStateException("Module-removal operation on WorkspaceSourceMetricsManager caused an attempt to remove missing key "+key+" from WorkspaceSourceMetricsManager tracking");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int getGemReferenceFrequency(QualifiedName gemName) {
        Integer result = gemToReferenceFrequencyMap.get(gemName);
        if (result != null) {
            return result.intValue();
        } else {
            return 0;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public String dumpCompositionalFrequencies(ModuleFilter moduleFilter, QualifiedNameFilter functionFilter, boolean traceSkippedFunctions) {
        StringBuilder buffer = new StringBuilder();
        Map<Pair<QualifiedName, QualifiedName>, Integer> workspaceCompositionalFrequencies = new HashMap<Pair<QualifiedName, QualifiedName>, Integer>(); 
            
        for(int i = 0; i < moduleContainer.getNModules(); i++) {
            ModuleTypeInfo moduleTypeInfo = moduleContainer.getNthModuleTypeInfo(i);

            if(moduleFilter.acceptModule(moduleTypeInfo.getModuleName())) {

                // Parse a SourceModel to process
                CompilerMessageLogger logger = new MessageLogger();
                SourceModel.ModuleDefn moduleDefn = moduleContainer.getSourceModel(moduleTypeInfo.getModuleName(), false, logger);
    
                // Fold this module's frequencies into the grand total
                Map<Pair<QualifiedName, QualifiedName>, Integer> compositionalFrequencies = SourceMetricFinder.computeCompositionalFrequencies(moduleDefn, moduleTypeInfo, functionFilter, traceSkippedFunctions); 
                addFrequencyMetrics(compositionalFrequencies, workspaceCompositionalFrequencies);
            } else {
                System.out.println("Skipping test module " + moduleTypeInfo.getModuleName());
            }
        }
            
        // Build a string from the grand total
        for (final Map.Entry<Pair<QualifiedName, QualifiedName>, Integer> entry : workspaceCompositionalFrequencies.entrySet()) {
            Pair<QualifiedName, QualifiedName> key = entry.getKey();
            Integer frequency = entry.getValue();
            
            buffer.append(key.fst());
            buffer.append(",");
            buffer.append(key.snd());
            buffer.append(",");
            buffer.append(frequency.intValue());
            buffer.append("\n");
        }

        return buffer.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized String dumpReferenceFrequencies(ModuleFilter moduleFilter, QualifiedNameFilter functionFilter, boolean traceSkippedFunctions) {
        StringBuilder buffer = new StringBuilder();
        Set<Map.Entry<QualifiedName, Integer>> dataEntries = null;
        
        if (moduleFilter instanceof AcceptAllModulesFilter && functionFilter instanceof AcceptAllQualifiedNamesFilter) {
            // If there are no filtering options selected, then we can just dump the cache
            dataEntries = gemToReferenceFrequencyMap.entrySet();
            
        } else {
            Map<Pair<QualifiedName, QualifiedName>, Integer> workspaceDependeeFrequencies = new HashMap<Pair<QualifiedName, QualifiedName>, Integer>();
            Map<QualifiedName, Integer> workspaceReferenceFrequencies = new HashMap<QualifiedName, Integer>();
            
            for(int i = 0; i < moduleContainer.getNModules(); i++) {
                ModuleTypeInfo moduleTypeInfo = moduleContainer.getNthModuleTypeInfo(i);
                
                if(moduleFilter.acceptModule(moduleTypeInfo.getModuleName())) {
    
                    // Parse a SourceModel to process
                    SourceModel.ModuleDefn moduleDefn = moduleContainer.getSourceModel(moduleTypeInfo.getModuleName(), false, new MessageLogger());
        
                    // Fold this module's frequencies into the grand total
                    Map<Pair<QualifiedName, QualifiedName>, Integer> referenceFrequencies = SourceMetricFinder.computeReferenceFrequencies(moduleDefn, moduleTypeInfo, functionFilter, traceSkippedFunctions); 
                    addFrequencyMetrics(referenceFrequencies, workspaceDependeeFrequencies);
                    
                } else {
                    System.out.println("Skipping test module " + moduleTypeInfo.getModuleName());
                }
            }

            // Summarize the dependee data into reference frequency data
            for (final Map.Entry<Pair<QualifiedName, QualifiedName>, Integer> entry : workspaceDependeeFrequencies.entrySet()) {
                Pair<QualifiedName, QualifiedName> dependeePair = entry.getKey();
                Integer frequency = entry.getValue();
                QualifiedName dependee = dependeePair.fst();
                
                addFrequencyEntry(dependee, frequency.intValue(), workspaceReferenceFrequencies);
            }

            dataEntries = workspaceReferenceFrequencies.entrySet();
        }
                
        // Iterate over the possibly-filtered collection and dump them out
        for (final Map.Entry<QualifiedName, Integer> entry : dataEntries) {
            QualifiedName name = entry.getKey();
            Integer frequency = entry.getValue();
            
            buffer.append(name.getModuleName());
            buffer.append(",");
            buffer.append(name.getUnqualifiedName());
            buffer.append(",");
            buffer.append(frequency.intValue());
            buffer.append("\n");
        }
        
        return buffer.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    public void dumpLintWarnings(ModuleFilter moduleFilter, QualifiedNameFilter functionFilter, boolean traceSkippedModulesAndFunctions, 
                                 boolean includeUnplingedPrimitiveArgs, boolean includeRedundantLambdas, boolean includeUnusedPrivates, boolean includeMismatchedWrapperPlings, boolean includeUnreferencedLetVariables) {
        
        
        for(int i = 0; i < moduleContainer.getNModules(); i++) {
            ModuleTypeInfo moduleTypeInfo = moduleContainer.getNthModuleTypeInfo(i);

            if(moduleFilter.acceptModule(moduleTypeInfo.getModuleName())) {

                // Parse a SourceModel to process
                SourceModel.ModuleDefn moduleDefn = moduleContainer.getSourceModel(moduleTypeInfo.getModuleName(), false,  new MessageLogger());
                
                // Dump the warnings
                List<LintWarning> warnings = SourceMetricFinder.computeLintWarnings(moduleDefn, moduleTypeInfo, functionFilter, traceSkippedModulesAndFunctions, 
                                                                       includeUnplingedPrimitiveArgs, includeRedundantLambdas, includeUnusedPrivates, includeMismatchedWrapperPlings, includeUnreferencedLetVariables);
                for (final LintWarning lintWarning : warnings) {
                    System.out.println(lintWarning.toString());
                }
            } else if(traceSkippedModulesAndFunctions) {
                System.out.println("Skipping test module " + moduleTypeInfo.getModuleName());
            }
        }
    }

    /**
     * Helper method for all the different kinds of search.  Walks over each 
     * module in the workspace, performing the specified SourceMetricFinder search
     * on each module where the target entity is visible. 
     * @param targetEntityList List of ScopedEntities to search for
     * @param searchType Type of search to perform
     * @return List of SearchResults of search hits
     */
    private List<SearchResult> performSearch(List<? extends ScopedEntity> targetEntityList, SourceMetricFinder.SearchType searchType, CompilerMessageLogger messageLogger) {
        
        if(targetEntityList.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<SearchResult> searchResults = new ArrayList<SearchResult>();

        // Process modules in sorted order to ensure that the results come
        // back sorted by module name.
        for (final ModuleTypeInfo moduleTypeInfo : getSortedModuleTypeInfos()) {
            Set<QualifiedName> qualifiedTargets = new HashSet<QualifiedName>();
            
            for (final ScopedEntity scopedEntity : targetEntityList) {
                if(isEntityVisibleToModule(scopedEntity, moduleTypeInfo)) {
                    qualifiedTargets.add(scopedEntity.getName());
                }
            }
            
            if(qualifiedTargets.size() == 0) {
                continue;
            }
            
            List<QualifiedName> targetNames = new ArrayList<QualifiedName>(qualifiedTargets);
            
            if(!moduleContainsHits(moduleTypeInfo, targetNames, searchType)) {
                continue;
            }
            
            // Parse a SourceModel to process
            MessageLogger moduleLogger = new MessageLogger();
            ModuleName moduleName = moduleTypeInfo.getModuleName();
            
            SourceModel.ModuleDefn moduleDefn = moduleContainer.getSourceModel(moduleName, false, moduleLogger);
            // if the module is a sourceless module, then we need to process it differently.
            if (moduleDefn == null && moduleLogger.getNErrors() == 0) {
                searchResults.addAll(performSourcelessSearch(moduleTypeInfo, targetNames, searchType));
                continue;
            }
            
            if(moduleDefn == null) {
                // Copy messages, adding source names to each SourcePosition
                for (final CompilerMessage compilerMessage : moduleLogger.getCompilerMessages()) {
                    SourceRange sourceRange = compilerMessage.getSourceRange(); 
                    if(sourceRange != null) {
                        SourceRange augmentedRange = getAugmentedRange(sourceRange, moduleName); 
                        messageLogger.logMessage(new CompilerMessage(augmentedRange, compilerMessage.getMessageKind()));
                    } else {
                        messageLogger.logMessage(compilerMessage);
                    }
                }
                continue;
            }
            
            // Find all the hits in the module and absorb them into the full total
            List<SearchResult.Precise> moduleSearchResults = SourceMetricFinder.performSearch(moduleDefn, moduleTypeInfo, searchType, targetNames);
            searchResults.addAll(augmentSearchResultsWithContextLines(moduleSearchResults, moduleContainer.getModuleSource(moduleName)));
        }
    
        return Collections.unmodifiableList(searchResults);
    }

    /**
     * Return a source range the same as the given one except that the module name is updated.
     * @param sourceRange the source range to add the module name to
     * @param moduleName the module name to add to the source range
     * @return a new source range that sames as the given one except that the module name is updated.
     */
    private SourceRange getAugmentedRange(SourceRange sourceRange, ModuleName moduleName){
        SourcePosition augmentedStartPosition = new SourcePosition(sourceRange.getStartLine(), sourceRange.getStartColumn(), moduleName);
        SourcePosition augmentedEndPosition = new SourcePosition(sourceRange.getEndLine(), sourceRange.getEndColumn(), moduleName);
        return new SourceRange(augmentedStartPosition, augmentedEndPosition);
    }

    /**
     * Helper method for all the different kinds of search. Walks over each
     * module in the workspace, performing the specified SourceMetricFinder
     * search on each module where the target entity is visible.
     * 
     * @param targetEntityList
     *            List of ScopedEntities to search for
     * @param targetModuleTypeInfos The list of moduleTypeInfos to search in the order to be searched.
     * @param searchType Type of search to perform
     * @param monitor A callback function to receive information about the progression of the search. This may be null. 
     */
    private void performSearch(
            List<? extends ScopedEntity> targetEntityList,
            Collection<ModuleTypeInfo> targetModuleTypeInfos, 
            SourceMetricFinder.SearchType searchType,
            IProgressMonitor monitor,
            CompilerMessageLogger messageLogger) {

        if (targetEntityList.isEmpty()) {
            return;
        }

        if (monitor != null){
            monitor.numberOfModules(targetModuleTypeInfos.size());
        }
        
        // Process modules in sorted order to ensure that the results come
        // back sorted by module name.
        for (final ModuleTypeInfo moduleTypeInfo : targetModuleTypeInfos) {
            if (monitor != null){
                monitor.startModule(moduleTypeInfo.getModuleName());
                if (monitor.isCancelled()){
                    return;
                }
            }            
            
            Set<QualifiedName> qualifiedTargets = new HashSet<QualifiedName>();

            for (final ScopedEntity scopedEntity : targetEntityList) {
                if (isEntityVisibleToModule(scopedEntity, moduleTypeInfo)) {
                    qualifiedTargets.add(scopedEntity.getName());
                }
            }

            if (qualifiedTargets.size() == 0) {
                continue;
            }

            List<QualifiedName> targetNames = new ArrayList<QualifiedName>(qualifiedTargets);

            if (!moduleContainsHits(moduleTypeInfo, targetNames,
                    searchType)) {
                continue;
            }

            // Parse a SourceModel to process
            MessageLogger moduleLogger = new MessageLogger();
            ModuleName moduleName = moduleTypeInfo.getModuleName();

            SourceModel.ModuleDefn moduleDefn = moduleContainer.getSourceModel(moduleName, false, moduleLogger);
            // if the module is a sourceless module, then we need to process it
            // differently.
            if (moduleDefn == null && moduleLogger.getNErrors() == 0) {
                monitor.moreResults(performSourcelessSearch(moduleTypeInfo, targetNames, searchType));
                continue;
            }


            if (moduleDefn == null) {
                // Copy messages, adding source names to each SourcePosition
                for (final CompilerMessage compilerMessage : moduleLogger.getCompilerMessages()) {
                    final SourceRange sourceRange = compilerMessage.getSourceRange();
                    if (sourceRange != null) {
                        SourceRange augmentedSourceRange = getAugmentedRange(sourceRange, moduleName);
                        messageLogger.logMessage(new CompilerMessage(
                            augmentedSourceRange, compilerMessage.getMessageKind()));
                    } else {
                        messageLogger.logMessage(compilerMessage);
                    }
                }
                continue;
            }

            // Find all the hits in the module and absorb them into the full
            // total
            List<SearchResult.Precise> moduleSearchResults = SourceMetricFinder.performSearch(
                    moduleDefn, moduleTypeInfo, searchType, targetNames);
            Collection<SearchResult> moreResults = 
                augmentSearchResultsWithContextLines(moduleSearchResults, moduleContainer.getModuleSource(moduleName));
            monitor.moreResults(moreResults);
        }
    }
    
    /**
     * Searches a sourceless module for references, definitions, or instances, and returns a list of SearchResult
     * representing the hits.
     * <p>
     * NOTE: this method is closely related to {@link #moduleContainsHits}. Modifying one should require modifications in the other.
     * 
     * @param moduleTypeInfo the ModuleTypeInfo for the sourceless module to be searched.
     * @param targetNames the List of QualifiedNames of the entities to search for.
     * @param searchType type of search to perform.
     * @return a List of the search results.
     */
    private List<SearchResult.Frequency> performSourcelessSearch(ModuleTypeInfo moduleTypeInfo, List<QualifiedName> targetNames, SourceMetricFinder.SearchType searchType) {
        
        ModuleName moduleName = moduleTypeInfo.getModuleName();
        LinkedHashMap<Pair<QualifiedName, SearchResult.Frequency.Type>, Integer> results = new LinkedHashMap<Pair<QualifiedName, SearchResult.Frequency.Type>, Integer>();

        if(searchType == SourceMetricFinder.SearchType.REFERENCES || searchType == SourceMetricFinder.SearchType.REFERENCES_CONSTRUCTIONS || searchType == SourceMetricFinder.SearchType.ALL) {
            
            for(int i = 0; i < moduleTypeInfo.getNFunctions(); i++) {
                Function functionEntity = moduleTypeInfo.getNthFunction(i);
                for (final QualifiedName qualifiedName : targetNames) {
                    Map<QualifiedName, Integer> dependeeToFrequencyMap = functionEntity.getDependeeToFrequencyMap();
                    if(dependeeToFrequencyMap.containsKey(qualifiedName)) {
                        Integer frequency = dependeeToFrequencyMap.get(qualifiedName);
                        incrementFrequencyCountForSearchResult(results, qualifiedName, SearchResult.Frequency.Type.FUNCTION_REFERENCES, frequency.intValue());
                    }
                }
            }
        
            for(int i = 0; i < moduleTypeInfo.getNClassInstances(); i++) {
                ClassInstance classInstance = moduleTypeInfo.getNthClassInstance(i);

                for(int j = 0; j < classInstance.getNInstanceMethods(); j++) {
                    QualifiedName resolvingFunctionName = classInstance.getInstanceMethod(j);
                    
                    if (resolvingFunctionName != null) {

                        for (final QualifiedName qualifiedName : targetNames) {
                            if(resolvingFunctionName.equals(qualifiedName)) {
                                incrementFrequencyCountForSearchResult(results, qualifiedName, SearchResult.Frequency.Type.INSTANCE_METHOD_REFERENCES, 1);
                            }
                        }
                        
                        if(searchType == SourceMetricFinder.SearchType.ALL) {
                            for (final QualifiedName targetName : targetNames) {
                                TypeClass targetClass = moduleTypeInfo.getVisibleTypeClass(targetName);
                                
                                
                                List<Set<TypeClass>> constraintList = classInstance.getSuperclassPolymorphicVarConstraints();
                                for(int constraintIdx = 0; constraintIdx < constraintList.size(); constraintIdx++) {
                                    Set<TypeClass> constraints = constraintList.get(constraintIdx);
                                    
                                    if(constraints.contains(targetClass)) {
                                        incrementFrequencyCountForSearchResult(results, targetName, SearchResult.Frequency.Type.CLASS_CONSTRAINTS, 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } 

        if(searchType == SourceMetricFinder.SearchType.INSTANCES || searchType == SourceMetricFinder.SearchType.ALL) {
            
            for(int i = 0; i < moduleTypeInfo.getNClassInstances(); i++) {
                ClassInstance classInstance = moduleTypeInfo.getNthClassInstance(i);
                for (final QualifiedName qualifiedName : targetNames) {
                    if(classInstance.getTypeClass().getName().equals(qualifiedName)) {
                        incrementFrequencyCountForSearchResult(results, qualifiedName, SearchResult.Frequency.Type.INSTANCES, 1);
                    }
                }
            }
        } 
        
        if(searchType == SourceMetricFinder.SearchType.CLASSES || searchType == SourceMetricFinder.SearchType.ALL) {
            
            for(int i = 0; i < moduleTypeInfo.getNClassInstances(); i++) {
                ClassInstance classInstance = moduleTypeInfo.getNthClassInstance(i);
                for (final QualifiedName qualifiedName : targetNames) {
                    TypeExpr typeExpr = classInstance.getType();

                    if(typeExpr.getArity() > 0 && qualifiedName.equals(CAL_Prelude.TypeConstructors.Function)) {
                        incrementFrequencyCountForSearchResult(results, qualifiedName, SearchResult.Frequency.Type.CLASSES, 1);
                    }
                    
                    TypeConsApp rootTypeConsApp = typeExpr.rootTypeConsApp();
                    if(rootTypeConsApp == null) {
                        continue;
                    }
                    
                    if(rootTypeConsApp.getName().equals(qualifiedName)) {
                        incrementFrequencyCountForSearchResult(results, qualifiedName, SearchResult.Frequency.Type.CLASSES, 1);
                    }
                }
            }
        }
            
        if(searchType == SourceMetricFinder.SearchType.DEFINITION || searchType == SourceMetricFinder.SearchType.ALL) {
            for (final QualifiedName qualifiedName : targetNames) {
                if(qualifiedName.getModuleName().equals(moduleTypeInfo.getModuleName())) {
                    incrementFrequencyCountForSearchResult(results, qualifiedName, SearchResult.Frequency.Type.DEFINITION, 1);
                }
            }
        }
        
        if(searchType == SourceMetricFinder.SearchType.ALL) {
        
            for (final QualifiedName qualifiedName : targetNames) {
                if(moduleTypeInfo.doesImportedNameOccur(qualifiedName)) {
                    incrementFrequencyCountForSearchResult(results, qualifiedName, SearchResult.Frequency.Type.IMPORT, 1);
                }
            }
        }
        
        List<SearchResult.Frequency> resultList = new ArrayList<SearchResult.Frequency>();
        for (final Map.Entry<Pair<QualifiedName, Type>, Integer> entry : results.entrySet()) {
            Pair<QualifiedName, SearchResult.Frequency.Type> targetNameAndType = entry.getKey();
            QualifiedName qualifiedName = targetNameAndType.fst();
            SearchResult.Frequency.Type type = targetNameAndType.snd();
            
            int frequency = entry.getValue().intValue();
            
            resultList.add(new SearchResult.Frequency(moduleName, qualifiedName, frequency, type));
        }
        
        return resultList;
    }
    
    /**
     * Helper method for incrementing the frequency count for a particular kind of search hit in a sourceless module.
     * @param map the map to be modified, mapping Pair<QualifiedName, SearchResult.Frequency.Type> to an Integer representing
     *            the frequency of that particular kind of search hit. 
     * @param targetName the name of the search hit's target. 
     * @param type the type of the search hit.
     * @param freq the frequency increment.
     */
    private void incrementFrequencyCountForSearchResult(LinkedHashMap<Pair<QualifiedName, SearchResult.Frequency.Type>, Integer> map, QualifiedName targetName, SearchResult.Frequency.Type type, int freq) {
        Pair<QualifiedName, SearchResult.Frequency.Type> key = new Pair<QualifiedName, SearchResult.Frequency.Type>(targetName, type);
        
        Integer value = map.get(key);
        if (value == null) {
            map.put(key, Integer.valueOf(freq));
        } else {
            map.put(key, Integer.valueOf(value.intValue() + freq));
        }
    }

    /**
     * @return The ModuleTypeInfos in the current workspace, sorted by
     *          module name.
     */
    public SortedSet<ModuleTypeInfo> getSortedModuleTypeInfos() {
        
        SortedSet<ModuleTypeInfo> sortedModuleTypeInfos = new TreeSet<ModuleTypeInfo>(new Comparator<ModuleTypeInfo>() {
            public int compare(ModuleTypeInfo a, ModuleTypeInfo b) {
                ModuleName leftModuleName = a.getModuleName();
                ModuleName rightModuleName = b.getModuleName();
                
                return leftModuleName.compareTo(rightModuleName);
            }
        });
        
        for(int i = 0; i < moduleContainer.getNModules(); i++) {
            sortedModuleTypeInfos.add(moduleContainer.getNthModuleTypeInfo(i));
        }

        return sortedModuleTypeInfos;
    }
    
    /**
     * Helper function that accepts a list of SearchResults and returns a new list of search results
     * with contextLines filled in.  Assumes that the SearchResults have valid SourcePositions that
     * refer to locations in moduleSource.
     * 
     * @param searchResults List of SearchResults to return new, augmented versions of
     * @param moduleSource String containing the source text that corresponds to the SourcePositions in
     *         the SearchResults of searchResults.
     * @return List of SearchResults with their contextLines set based on their SourcePositions in moduleSource
     */
    private static List<SearchResult> augmentSearchResultsWithContextLines(List<? extends SearchResult> searchResults, String moduleSource) {
        List<SearchResult> results = new ArrayList<SearchResult>(searchResults.size());
        
        SourcePosition previousPosition = null;
        int previousIndex = 0; 
        
        for (final SearchResult searchResult : searchResults) {
            if (searchResult instanceof SearchResult.Precise) {
                SearchResult.Precise preciseSearchResult = (SearchResult.Precise)searchResult;
                
                SourcePosition currentPosition = preciseSearchResult.getSourcePosition();
                
                SourcePosition lineStart = new SourcePosition(currentPosition.getLine(), 1, currentPosition.getSourceName());
                int startIndex;
                int currentIndex;
                
                if (previousPosition != null && SourcePosition.compareByPosition.compare(previousPosition, lineStart) <= 0) {
                    startIndex = lineStart.getPosition(moduleSource, previousPosition, previousIndex);
                    currentIndex = currentPosition.getPosition(moduleSource, lineStart, startIndex);
                    
                } else {
                    startIndex = lineStart.getPosition(moduleSource);
                    currentIndex = currentPosition.getPosition(moduleSource, lineStart, startIndex);
                }
                
                int endIndex = moduleSource.indexOf('\n', startIndex);
                String line = moduleSource.substring(startIndex, endIndex);
                
                previousPosition = lineStart;
                previousIndex = startIndex;

                results.add(new SearchResult.Precise(preciseSearchResult.getSourceRange(), preciseSearchResult.getName(), preciseSearchResult.getCategory(), line, currentIndex - startIndex, preciseSearchResult.refersToJavaSource()));
                
            } else {
                results.add(searchResult);
            }
        }
        
        return results;
    }
    
    /**
     * Helper function for pre-filtering modules before they have been parsed.  ModuleTypeInfo doesn't contain
     * enough data to tell where in a module's source text a hit occurs, but it does contain enough data to tell
     * whether or not there are any hits in the module.  Scanning this data is a lot faster than parsing a module
     * into a SourceModel and walking it, so before we parse each module we do a quick check to see if there are
     * any hits.
     * <p>
     * NOTE: this method is closely related to {@link #performSourcelessSearch}. Modifying one should require modifications in the other.
     * 
     * @param moduleTypeInfo ModuleTypeInfo of module to check for hits
     * @param targetNames List of QualifiedNames of search targets
     * @param searchType Type of search being performed
     * @return boolean true if the module contains at least one search hit for the specified search type and targets, or false otherwise.
     */
    private static boolean moduleContainsHits(ModuleTypeInfo moduleTypeInfo, List<QualifiedName> targetNames, SourceMetricFinder.SearchType searchType) {
        
        if(searchType == SourceMetricFinder.SearchType.REFERENCES || searchType == SourceMetricFinder.SearchType.REFERENCES_CONSTRUCTIONS || searchType == SourceMetricFinder.SearchType.ALL) {
            
            for(int i = 0; i < moduleTypeInfo.getNFunctions(); i++) {
                Function functionEntity = moduleTypeInfo.getNthFunction(i);
                for (final QualifiedName qualifiedName : targetNames) {
                    // Short-circuit return as soon as we know that there is a reference to one
                    // of the target names in this module.
                    if(functionEntity.getDependeeToFrequencyMap().keySet().contains(qualifiedName)) {
                        return true;
                    }
                }
            }
        
            for(int i = 0; i < moduleTypeInfo.getNClassInstances(); i++) {
                ClassInstance classInstance = moduleTypeInfo.getNthClassInstance(i);

                for(int j = 0; j < classInstance.getNInstanceMethods(); j++) {
                    QualifiedName resolvingFunctionName = classInstance.getInstanceMethod(j);
                    
                    if (resolvingFunctionName != null) {

                        for (final QualifiedName qualifiedName : targetNames) {
                            // Short-circuit return as soon as we find an instance method that
                            // resolves to one of the target names
                            if(resolvingFunctionName.equals(qualifiedName)) {
                                return true;
                            }
                        }
                        
                        if(searchType == SourceMetricFinder.SearchType.ALL) {
                            for (final QualifiedName targetName : targetNames) {
                                TypeClass targetClass = moduleTypeInfo.getVisibleTypeClass(targetName);
                                
                                
                                List<Set<TypeClass>> constraintArray = classInstance.getSuperclassPolymorphicVarConstraints();
                                for(int constraintIdx = 0; constraintIdx < constraintArray.size(); constraintIdx++) {
                                    Set<TypeClass> constraints = constraintArray.get(constraintIdx);
                                    
                                    if(constraints.contains(targetClass)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } 

        if(searchType == SourceMetricFinder.SearchType.INSTANCES || searchType == SourceMetricFinder.SearchType.ALL) {
            
            for(int i = 0; i < moduleTypeInfo.getNClassInstances(); i++) {
                ClassInstance classInstance = moduleTypeInfo.getNthClassInstance(i);
                for (final QualifiedName qualifiedName : targetNames) {
                    // Short-circuit return as soon as we know that there is an instance of
                    // one of the target classes in the module
                    if(classInstance.getTypeClass().getName().equals(qualifiedName)) {
                        return true;
                    }
                }
            }
        } 
        
        if(searchType == SourceMetricFinder.SearchType.CLASSES || searchType == SourceMetricFinder.SearchType.ALL) {
            
            for(int i = 0; i < moduleTypeInfo.getNClassInstances(); i++) {
                ClassInstance classInstance = moduleTypeInfo.getNthClassInstance(i);
                for (final QualifiedName qualifiedName : targetNames) {
                    TypeExpr typeExpr = classInstance.getType();

                    if(typeExpr.getArity() > 0 && qualifiedName.equals(CAL_Prelude.TypeConstructors.Function)) {
                        return true;
                    }
                    
                    TypeConsApp rootTypeConsApp = typeExpr.rootTypeConsApp();
                    if(rootTypeConsApp == null) {
                        continue;
                    }
                    
                    if(rootTypeConsApp.getName().equals(qualifiedName)) {
                        return true;
                    }
                }
            }
        }
            
        if(searchType == SourceMetricFinder.SearchType.DEFINITION || searchType == SourceMetricFinder.SearchType.ALL) {
            for (final QualifiedName qualifiedName : targetNames) {
                if(qualifiedName.getModuleName().equals(moduleTypeInfo.getModuleName())) {
                    return true;
                }
            }
        }
        
        if(searchType == SourceMetricFinder.SearchType.ALL) {
        
            for (final QualifiedName qualifiedName : targetNames) {
                if(moduleTypeInfo.doesImportedNameOccur(qualifiedName)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check whether an entity can be seen in the specified module 
     * @param scopedEntity ScopedEntityImpl entity to check visibility of
     * @param moduleTypeInfo ModuleTypeInfo of module to check for visibility from
     * @return true if the module specified by metaModule can see the gem specified by envEntity,
     *          or false otherwise.
     */
    private static boolean isEntityVisibleToModule(ScopedEntity scopedEntity, ModuleTypeInfo moduleTypeInfo) {        
        return moduleTypeInfo.isEntityVisible(scopedEntity);
    }

    /**
     * Find all the instances of the given class. 
     * The monitor is called with incremental updates to the search results.
     * @param targetName name (as a String) of the gem to look for references to
     * @param moduleTypeInfos A list of the ModuleTypeInfo's of the modules to search.
     * @param monitor An object that is notified of progress of the search.
     * @param messageLogger Used to log messages describing conditions encountered
     *                       during searching (eg, unparseable module file encountered).
     *                       This argument must not be null.
     */
    public void findInstanceOfClass(String targetName, Collection<ModuleTypeInfo> moduleTypeInfos, IProgressMonitor monitor, CompilerMessageLogger messageLogger){
        if (messageLogger == null){
            throw new NullPointerException("messageLogger must not be null");
        }
        
        List<ScopedEntity> targetEntities = findMatchingEntities(targetName);
        performSearch(targetEntities, moduleTypeInfos, SourceMetricFinder.SearchType.INSTANCES, monitor, messageLogger);
    }
    
    /**
     * Finds all of the entities in the workspace that have the specified unqualified name
     * @param targetName String possibly-wildcarded name to search for
     * @return List of ScopedEntity of entities with the specified unqualified name
     */
    private List<ScopedEntity> findMatchingEntities(String targetName) {
        
        String unqualifiedName = targetName;
        Matcher moduleMatcher = null;
        int indexOfPeriod = targetName.lastIndexOf('.');
        if(indexOfPeriod != -1) {
            unqualifiedName = targetName.substring(indexOfPeriod + 1);
            String moduleName = targetName.substring(0, indexOfPeriod);
            // we prepend the pattern with (.+\.)? so that if the user specified C.f, then B.C.f and A.B.C.f would also match
            String modulePattern = "(.+\\.)?" + WildcardPatternMatcher.wildcardPatternToRegExp(moduleName);
            moduleMatcher = Pattern.compile(modulePattern, Pattern.CASE_INSENSITIVE).matcher("");
        }
        
        Matcher unqualifiedMatcher = Pattern.compile(WildcardPatternMatcher.wildcardPatternToRegExp(unqualifiedName), Pattern.CASE_INSENSITIVE).matcher(""); 

        List<ScopedEntity> results = new ArrayList<ScopedEntity>();
        
        // Process modules in sorted order to ensure that the results come
        // back sorted by module name.
        for (final ModuleTypeInfo moduleTypeInfo : getSortedModuleTypeInfos()) {
            if(moduleMatcher != null) {
                moduleMatcher.reset(moduleTypeInfo.getModuleName().toSourceText());
                if(!moduleMatcher.matches()) {
                    continue;
                }
            }
            
            for(int j = 0; j < moduleTypeInfo.getNTypeClasses(); j++) {
                TypeClass typeClass = moduleTypeInfo.getNthTypeClass(j);
                unqualifiedMatcher.reset(typeClass.getName().getUnqualifiedName());
                if(unqualifiedMatcher.matches()) {
                    results.add(typeClass);
                }
            }

            for(int j = 0; j < moduleTypeInfo.getNTypeConstructors(); j++) {
                TypeConstructor typeConstructor = moduleTypeInfo.getNthTypeConstructor(j);
                unqualifiedMatcher.reset(typeConstructor.getName().getUnqualifiedName());
                if(unqualifiedMatcher.matches()) {
                    results.add(typeConstructor);
                }
            }

            FunctionalAgent[] envEntities = moduleTypeInfo.getFunctionalAgents();
            for (final FunctionalAgent envEntity : envEntities) {
                unqualifiedMatcher.reset(envEntity.getName().getUnqualifiedName());
                if(unqualifiedMatcher.matches()) {
                    results.add(envEntity);
                }
            }
        }    
        
        return results;
    }
    
    private List<ScopedEntity> findMatchingEntities(QualifiedName qualifiedName) {
        
        // Ensure that the target gem is visible before doing any serious work
        ModuleTypeInfo homeModuleTypeInfo = moduleContainer.getModuleTypeInfo(qualifiedName.getModuleName());
        if(homeModuleTypeInfo == null){
            return Collections.emptyList();
        }
        
        List<ScopedEntity> targetList = new ArrayList<ScopedEntity>();
        
        FunctionalAgent envEntity = homeModuleTypeInfo.getFunctionalAgent(qualifiedName.getUnqualifiedName()); 
        if(envEntity != null) {
            targetList.add(envEntity); 
        }
        
        TypeClass typeClass = homeModuleTypeInfo.getTypeClass(qualifiedName.getUnqualifiedName());
        if(typeClass != null) {
            targetList.add(typeClass); 
        }
        
        TypeConstructor typeCons = homeModuleTypeInfo.getTypeConstructor(qualifiedName.getUnqualifiedName());
        if(typeCons != null) {
            targetList.add(typeCons); 
        }

        return targetList;
    }
    
    /**
     * {@inheritDoc}
     * TODOJ The QualifiedName-based API is no longer used and may want to be deleted
     */
    public List<SearchResult> findAllOccurrences(QualifiedName targetGem, CompilerMessageLogger messageLogger) {
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }
        
        List<ScopedEntity> targetEntities = findMatchingEntities(targetGem);
        return performSearch(targetEntities, SourceMetricFinder.SearchType.ALL, messageLogger);
    }
    
    /**
     * {@inheritDoc}
     */
    public List<SearchResult> findAllOccurrences(String targetName, CompilerMessageLogger messageLogger) {
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        List<ScopedEntity> targetEntities = findMatchingEntities(targetName);
        return performSearch(targetEntities, SourceMetricFinder.SearchType.ALL, messageLogger);
    }

    /**
     * Find all the occurrences of the specified ScopedEntityImpl.
     * The monitor is called with incremental updates to the search results.
     * @param targetName name (as a String) of the entity to find occurrences of
     * @param moduleTypeInfos A list of the ModuleTypeInfo's of the modules to search.
     * @param monitor An object that is notified of progress of the search.
     * @param messageLogger Used to log messages describing conditions encountered
     *                       during searching (eg, unparseable module file encountered).
     *                       This argument must not be null.
     */
    public void findAllOccurrences(String targetName,
            Collection<ModuleTypeInfo> moduleTypeInfos,
            IProgressMonitor monitor, 
            CompilerMessageLogger messageLogger) {
        if (messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        List<ScopedEntity> targetEntities = findMatchingEntities(targetName);
        performSearch(targetEntities, moduleTypeInfos, SourceMetricFinder.SearchType.ALL, monitor, messageLogger);
    }

    /**
     * {@inheritDoc}
     * TODOJ The QualifiedName-based API is no longer used and may want to be deleted
     */
    public List<SearchResult> findReferences(QualifiedName targetGem, CompilerMessageLogger messageLogger) {
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        List<ScopedEntity> targetEntities = findMatchingEntities(targetGem);
        return performSearch(targetEntities, SourceMetricFinder.SearchType.REFERENCES, messageLogger);
    }
    
    /**
     * {@inheritDoc}
     */
    public List<SearchResult> findReferences(String targetName, CompilerMessageLogger messageLogger) {
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        List<ScopedEntity> targetEntities = findMatchingEntities(targetName);
        return performSearch(targetEntities, SourceMetricFinder.SearchType.REFERENCES, messageLogger);
    }
    
    /**
     * Find all the references to the specified gem in the workspace and return a list
     * of SearchResults of reference to the gem.
     * The monitor is called with incremental updates to the search results.
     * @param targetName name (as a String) of the gem to look for references to
     * @param moduleTypeInfos A list of the ModuleTypeInfo's of the modules to search.
     * @param monitor An object that is notified of progress of the search.
     * @param messageLogger Used to log messages describing conditions encountered
     *                       during searching (eg, unparseable module file encountered).
     *                       This argument must not be null.
     */
    public void findReferences(String targetName,
            Collection<ModuleTypeInfo> moduleTypeInfos,
            IProgressMonitor monitor, 
            CompilerMessageLogger messageLogger) {
        if (messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        List<ScopedEntity> targetEntities = findMatchingEntities(targetName);
        performSearch(targetEntities, moduleTypeInfos, SourceMetricFinder.SearchType.REFERENCES, monitor, messageLogger);
    }

    /**
     * {@inheritDoc}
     * TODOJ The QualifiedName-based API is no longer used and may want to be deleted
     */
    public List<SearchResult> findInstancesOfClass(QualifiedName targetClass, CompilerMessageLogger messageLogger) {

        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        // Ensure that the target class is visible before doing any serious work
        ModuleTypeInfo homeModuleTypeInfo = moduleContainer.getModuleTypeInfo(targetClass.getModuleName());
        if(homeModuleTypeInfo == null) {
            return Collections.emptyList();
        }
        
        TypeClass typeClass = homeModuleTypeInfo.getTypeClass(targetClass.getUnqualifiedName()); 
        if(typeClass == null) {
            return Collections.emptyList();
        }

        return performSearch(Collections.<ScopedEntity>singletonList(typeClass), SourceMetricFinder.SearchType.INSTANCES, messageLogger);
    }

    /**
     * {@inheritDoc}
     */
    public List<SearchResult> findInstancesOfClass(String targetName, CompilerMessageLogger messageLogger) {
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        List<ScopedEntity> targetEntities = findMatchingEntities(targetName);
        return performSearch(targetEntities, SourceMetricFinder.SearchType.INSTANCES, messageLogger);
    }
    
    /**
     * {@inheritDoc}
     */
    public List<SearchResult> findTypeInstances(String targetName, CompilerMessageLogger messageLogger) {
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        List<ScopedEntity> targetEntities = findMatchingEntities(targetName);
        return performSearch(targetEntities, SourceMetricFinder.SearchType.CLASSES, messageLogger);
    }
    
    /**
     * Find all the instances associated with the specified type (i.e., find all of the type classes of
     * which the specified type is an instance) and return a list of SearchResults of the relevant 
     * instance definitions.
     * The monitor is called with incremental updates to the search results.
     * @param targetName name (as a String) of the gem to look for references to
     * @param moduleTypeInfos A list of the ModuleTypeInfo's of the modules to search.
     * @param monitor An object that is notified of progress of the search.
     * @param messageLogger Used to log messages describing conditions encountered
     *                       during searching (eg, unparseable module file encountered).
     *                       This argument must not be null.
     */
    public void findTypeInstances(String targetName,
            Collection<ModuleTypeInfo> moduleTypeInfos,
            IProgressMonitor monitor, 
            CompilerMessageLogger messageLogger) {
        if (messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        List<ScopedEntity> targetEntities = findMatchingEntities(targetName);
        performSearch(targetEntities, moduleTypeInfos, 
                SourceMetricFinder.SearchType.CLASSES, monitor, messageLogger);
    }

    
    /**
     * Find all constructions of the specified data constructor and return a list 
     * of SearchResults of the expressions.
     * The monitor is called with incremental updates to the search results.
     * @param targetName name (as a String) of the gem to look for constructions of
     * @param moduleTypeInfos A list of the ModuleTypeInfo's of the modules to search.
     * @param monitor An object that is notified of progress of the search.
     * @param messageLogger Used to log messages describing conditions encountered
     *                       during searching (eg, unparseable module file encountered).
     *                       This argument must not be null.
     */
    public void findConstructions(String targetName,
            Collection<ModuleTypeInfo> moduleTypeInfos,
            IProgressMonitor monitor, 
            CompilerMessageLogger messageLogger) {
        if (messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        final List<ScopedEntity> allTargetEntities = findMatchingEntities(targetName);
        final List<DataConstructor> targetEntities = convertToOnlyConstructors(allTargetEntities);
        performSearch(targetEntities, moduleTypeInfos, 
                SourceMetricFinder.SearchType.REFERENCES_CONSTRUCTIONS, monitor, messageLogger);
    }
    
    /**
     * If the specified target is a constructor then find all constructions of the specified constructor.
     * If the specified target is a type then final all constructions of any constructor that has the
     * given type.  
     * @param targetName name (as a String) of the constructor to look for constructions of 
     * or the type to look for constructions of its constructors. 
     * @param messageLogger Used to log messages describing conditions encountered
     *                       during searching (eg, unparseable module file encountered).
     *                       This argument must not be null.
     */
    public List<SearchResult> findConstructions(String targetName, CompilerMessageLogger messageLogger){
        final List<ScopedEntity> allTargetEntities = findMatchingEntities(targetName);
        final List<DataConstructor> targetEntities = convertToOnlyConstructors(allTargetEntities);
        return performSearch(targetEntities, SourceMetricFinder.SearchType.REFERENCES_CONSTRUCTIONS, messageLogger);
    }

    /**
     * @param entities list of entities to convert
     * @return a list of contructors from the given list as well as contructors of any types in the given list.
     */
    private List<DataConstructor> convertToOnlyConstructors(final List<ScopedEntity> entities) {
        final List<DataConstructor> targetEntities = new ArrayList<DataConstructor>();
        for (final ScopedEntity entity : entities) {
            if (entity instanceof DataConstructor){
                targetEntities.add((DataConstructor)entity);
            }
            else if (entity instanceof TypeConstructor){
                TypeConstructor typeConstructor = (TypeConstructor) entity;
                final int nDataConstructors = typeConstructor.getNDataConstructors();
                for(int n = 0; n < nDataConstructors; ++n){
                    targetEntities.add(typeConstructor.getNthDataConstructor(n));
                }
            }            
        }
        return targetEntities;
    }

    /**
     * {@inheritDoc}
     * TODOJ The QualifiedName-based API is no longer used and may want to be deleted
     */
    public List<SearchResult> findTypeInstances(QualifiedName targetType, CompilerMessageLogger messageLogger) {
        
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        // Ensure that the target type is visible before doing any serious work
        ModuleTypeInfo homeModuleTypeInfo = moduleContainer.getModuleTypeInfo(targetType.getModuleName());
        if(homeModuleTypeInfo == null){
            return Collections.emptyList();
        }
        
        TypeConstructor typeCons = homeModuleTypeInfo.getTypeConstructor(targetType.getUnqualifiedName());
        if(typeCons == null) {
            return Collections.emptyList();
        }
        
        return performSearch(Collections.<ScopedEntity>singletonList(typeCons), SourceMetricFinder.SearchType.CLASSES, messageLogger);
    }

    class ResultsCollector implements IProgressMonitor {

        private final ArrayList<SearchResult> results = new ArrayList<SearchResult>();
        
        public List<SearchResult> getResults(){
            return results;
        }
        
        public void numberOfModules(int numberOfModules) {
        }

        public void startModule(ModuleName module) {
        }

        public void done() {
        }

        public boolean isCancelled() {
            return false;
        }

        public void moreResults(Collection<? extends SearchResult> moreResults) {
            results.addAll(moreResults);
        }        
    }

    /**
     * {@inheritDoc}
     * TODOJ The QualifiedName-based API is no longer used and may want to be deleted
     */
    public List<SearchResult> findDefinition(Name targetName, CompilerMessageLogger messageLogger) {
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        ResultsCollector results = new ResultsCollector();
        findDefinitionHelper(Collections.singletonList(targetName), results, SourceMetricFinder.SearchType.DEFINITION, messageLogger);
        return results.getResults();
    }

    public List<SearchResult> findDefinition(SearchResult.Precise targetIdentifier, CompilerMessageLogger messageLogger) {
        return findDefinition(targetIdentifier, false, messageLogger);
    }
    
    /**
     * Find the definition of the given targetIdentifier.
     */
    public List<SearchResult> findDefinition(SearchResult.Precise targetIdentifier, boolean getSourceText, CompilerMessageLogger messageLogger) {
        if(messageLogger == null) {
            throw new NullPointerException("messageLogger must not be null");
        }

        ResultsCollector results = new ResultsCollector();
        SearchType searchType = SearchType.DEFINITION;
        if (getSourceText){
            searchType = SearchType.SOURCE_TEXT;
        }
        findDefinitionHelper(Collections.singletonList(targetIdentifier.getName()), results, searchType, messageLogger);

        final List<SearchResult> resultsList = results.getResults();
        
        SourceIdentifier.Category category = targetIdentifier.getCategory();
        
        // If there is not category information then return the unfiltered list.
        if (category == null){
            return resultsList;
        }

        // If category information is present use that to reduce the number of options.
        
        final List<SearchResult> filteredList = new ArrayList<SearchResult>();
        
        // LOCAL_DEFINITION and LOCAL_VARIABLE are the same for this purpose
        if (category == SourceIdentifier.Category.LOCAL_VARIABLE_DEFINITION){
            category = SourceIdentifier.Category.LOCAL_VARIABLE;
        }
        
        for (final SearchResult result : resu
