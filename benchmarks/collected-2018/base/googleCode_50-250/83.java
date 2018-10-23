// https://searchcode.com/api/result/8461501/

/*
 * Copyright 2007 Andrew Wills
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.danann.cernunnos.sql;

import java.sql.Connection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.danann.cernunnos.AttributePhrase;
import org.danann.cernunnos.EntityConfig;
import org.danann.cernunnos.Formula;
import org.danann.cernunnos.LiteralPhrase;
import org.danann.cernunnos.Phrase;
import org.danann.cernunnos.Reagent;
import org.danann.cernunnos.ReagentType;
import org.danann.cernunnos.SimpleFormula;
import org.danann.cernunnos.SimpleReagent;
import org.danann.cernunnos.Task;
import org.danann.cernunnos.TaskRequest;
import org.danann.cernunnos.TaskResponse;
import org.dom4j.Node;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public final class UpsertTask implements Task {

	// Instance Members.
	private Phrase dataSourcePhrase;
	private Phrase connectionPhrase;
	private Phrase update_sql;
	private Phrase insert_sql;
	private List<Phrase> parameters;
	private List<Phrase> update_parameters;
	private List<Phrase> insert_parameters;
	protected final Log log = LogFactory.getLog(this.getClass());

	/*
	 * Public API.
	 */

	public static final Reagent DATA_SOURCE = new SimpleReagent("DATA_SOURCE", "@data-source", ReagentType.PHRASE, DataSource.class,
            		"The DataSource to use for executing the SQL. If omitted the request attribute under the name " +
            		"'SqlAttributes.DATA_SOURCE' will be used", new AttributePhrase(SqlAttributes.DATA_SOURCE));

	public static final Reagent CONNECTION = new SimpleReagent("CONNECTION", "@connection", ReagentType.PHRASE, Connection.class,
					"**DEPRECATED:  Use DATA_SOURCE instead.**  Optional Connection object.  The default is the value of the " +
					"'SqlAttributes.CONNECTION' request attribute (if specified) or null.", 
					new AttributePhrase(SqlAttributes.CONNECTION, new LiteralPhrase(null)));

	public static final Reagent UPDATE_SQL = new SimpleReagent("UPDATE_SQL", "update-statement", ReagentType.PHRASE, String.class,
										"The SQL statement that performs the Update portion of the 'Upsert' operation.");

	public static final Reagent INSERT_SQL = new SimpleReagent("INSERT_SQL", "insert-statement", ReagentType.PHRASE, String.class,
										"The SQL statement that performs the Insert portion of the 'Upsert' operation.");

	public static final Reagent PARAMETERS = new SimpleReagent("PARAMETERS", "parameter/@value", ReagentType.NODE_LIST, List.class,
										"The parameters (if any) for the PreparedStatement objects that will perform this upsert.  "
										+ "WARNING:  Parameters must appear in the same order as the associated SQL.",
										Collections.emptyList());

	public static final Reagent UPDATE_PARAMETERS = new SimpleReagent("UPDATE_PARAMETERS", "update-parameter/@value", ReagentType.NODE_LIST, List.class,
										"If provided, UPDATE_PARAMETERS will override the PARAMETERS list for the 'update' operation only.  "
										+ "Use UPDATE_PARAMETERS and INSERT_PARAMETERS instead of PARAMETERS if update and insert parameters must "
										+ "differ in number or order.", null);

	public static final Reagent INSERT_PARAMETERS = new SimpleReagent("INSERT_PARAMETERS", "insert-parameter/@value", ReagentType.NODE_LIST, List.class,
										"If provided, INSERT_PARAMETERS will override the PARAMETERS list for the 'insert' operation only.  "
										+ "Use UPDATE_PARAMETERS and INSERT_PARAMETERS instead of PARAMETERS if update and insert parameters must "
										+ "differ in number or order.", null);

	public Formula getFormula() {
		Reagent[] reagents = new Reagent[] {DATA_SOURCE, CONNECTION, UPDATE_SQL, INSERT_SQL,
						PARAMETERS, UPDATE_PARAMETERS, INSERT_PARAMETERS};
		final Formula rslt = new SimpleFormula(UpsertTask.class, reagents);
		return rslt;
	}

	@SuppressWarnings("unchecked")
    public void init(EntityConfig config) {

		this.dataSourcePhrase = (Phrase) config.getValue(DATA_SOURCE);
		this.connectionPhrase = (Phrase) config.getValue(CONNECTION);
		this.update_sql = (Phrase) config.getValue(UPDATE_SQL);
		this.insert_sql = (Phrase) config.getValue(INSERT_SQL);

		List<Node> nodes = null;

		// PARAMETERS...
		this.parameters = new LinkedList<Phrase>();
		nodes = (List<Node>) config.getValue(PARAMETERS);
		for (final Node n : nodes) {
			parameters.add(config.getGrammar().newPhrase(n));
		}

		// UPDATE_PARAMETERS...
		nodes = (List<Node>) config.getValue(UPDATE_PARAMETERS);
		if (nodes != null) {
			this.update_parameters = new LinkedList<Phrase>();
			for (final Node n : nodes) {
				update_parameters.add(config.getGrammar().newPhrase(n));
			}
		} else {
			// Go with PARAMETERS...
			this.update_parameters = null;
		}

		// INSERT_PARAMETERS...
		nodes = (List<Node>) config.getValue(INSERT_PARAMETERS);
		if (nodes != null) {
			this.insert_parameters = new LinkedList<Phrase>();
            for (final Node n : nodes) {
				insert_parameters.add(config.getGrammar().newPhrase(n));
			}
		} else {
			// Go with PARAMETERS...
			this.insert_parameters = null;
		}

	}

	public void perform(TaskRequest req, TaskResponse res) {
	    final DataSource dataSource = DataSourceRetrievalUtil.getDataSource(dataSourcePhrase, connectionPhrase, req, res);
		
        final SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
        final JdbcOperations jdbcOperations = jdbcTemplate.getJdbcOperations();
        
        final int updateResult = this.doUpdate(jdbcOperations, req, res);
        
        // No information present, add the row...
        if (updateResult == 0) {
            this.doInsert(jdbcOperations, req, res);
        }
	}

    /**
     * Executes the update and returns the affected row count
     * 
     * Moved to its own method to reduce possibility for variable name confusion
     */
    protected int doUpdate(JdbcOperations jdbcOperations, TaskRequest req, TaskResponse res) {
        //Setup the update parameters and setter
        final List<Phrase> parametersInUse = update_parameters != null ? update_parameters : parameters;
        final PreparedStatementSetter preparedStatementSetter = new PhraseParameterPreparedStatementSetter(parametersInUse, req, res);
        
        //Get the update sql and execute the update.
        final String fUpdateSql = (String) update_sql.evaluate(req, res);
        return jdbcOperations.update(fUpdateSql, preparedStatementSetter);
    }
    
    /**
     * Executes the insert and returns the affected row count
     * 
     * Moved to its own method to reduce possibility for variable name confusion
     */
    protected int doInsert(JdbcOperations jdbcOperations, TaskRequest req, TaskResponse res) {
        //Setup the insert parameters and setter
        final List<Phrase> parametersInUse = insert_parameters != null ? insert_parameters : parameters;
        final PreparedStatementSetter preparedStatementSetter = new PhraseParameterPreparedStatementSetter(parametersInUse, req, res);
        
        //Get the insert sql and execute the insert
        final String fInsertSql = (String) insert_sql.evaluate(req, res);
        return jdbcOperations.update(fInsertSql, preparedStatementSetter);
    }
}
