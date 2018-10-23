// https://searchcode.com/api/result/109203704/

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.jndi;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.directory.DirContext;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapName;
import javax.naming.spi.DirStateFactory;
import javax.naming.spi.DirectoryManager;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.OperationManager;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.event.DirectoryListener;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.JndiPropertyConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.cursor.SingletonCursor;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.jndi.JndiUtils;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.AVA;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A non-federated abstract Context implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class ServerContext implements EventContext
{
    /** property key used for deleting the old RDN on a rename */
    public static final String DELETE_OLD_RDN_PROP = JndiPropertyConstants.JNDI_LDAP_DELETE_RDN;

    /** Empty array of controls for use in dealing with them */
    protected static final Control[] EMPTY_CONTROLS = new Control[0];

    /** The directory service which owns this context **/
    private final DirectoryService service;

    /** The cloned environment used by this Context */
    private final Hashtable<String, Object> env;

    /** The distinguished name of this Context */
    private final DN dn;

    /** The set of registered NamingListeners */
    private final Map<NamingListener,DirectoryListener> listeners = 
        new HashMap<NamingListener,DirectoryListener>();

    /** The request controls to set on operations before performing them */
    protected Control[] requestControls = EMPTY_CONTROLS;

    /** The response controls to set after performing operations */
    protected Control[] responseControls = EMPTY_CONTROLS;

    /** Connection level controls associated with the session */
    protected Control[] connectControls = EMPTY_CONTROLS;
    
    private final CoreSession session;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    
    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.  This
     * specific contstructor relies on the presence of the {@link
     * Context#PROVIDER_URL} key and value to determine the distinguished name
     * of the newly created context.  It also checks to make sure the
     * referenced name actually exists within the system.  This constructor
     * is used for all InitialContext requests.
     * 
     * @param service the parent service that manages this context
     * @param env the environment properties used by this context.
     * @throws NamingException if the environment parameters are not set 
     * correctly.
     */
    protected ServerContext( DirectoryService service, Hashtable<String, Object> env ) throws Exception
    {
        this.service = service;

        this.env = env;
        
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( this.env );
        dn = props.getProviderDn();

        /*
         * Need do bind operation here, and bindContext returned contains the 
         * newly created session.
         */
        BindOperationContext bindContext = doBindOperation( props.getBindDn(), props.getCredentials(), 
            props.getSaslMechanism(), props.getSaslAuthId() );

        session = bindContext.getSession();
        OperationManager operationManager = service.getOperationManager();
        
        if ( ! operationManager.hasEntry( new EntryOperationContext( session, dn ) ) )
        {
            throw new NameNotFoundException( I18n.err( I18n.ERR_490, dn ) );
        }
    }
    
    
    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.  This
     * constructor is used to propagate new contexts from existing contexts.
     *
     * @param principal the directory user principal that is propagated
     * @param dn the distinguished name of this context
     * @param service the directory service core
     * @throws NamingException if there is a problem creating the new context
     */
    public ServerContext( DirectoryService service, LdapPrincipal principal, Name name ) throws Exception
    {
        this.service = service;
        this.dn = (DN)(DN.fromName( name ).clone());

        this.env = new Hashtable<String, Object>();
        this.env.put( PROVIDER_URL, dn.toString() );
        this.env.put( DirectoryService.JNDI_KEY, service );
        session = new DefaultCoreSession( principal, service );
        OperationManager operationManager = service.getOperationManager();
        
        if ( ! operationManager.hasEntry( new EntryOperationContext( session, dn ) ) )
        {
            throw new NameNotFoundException( I18n.err( I18n.ERR_490, dn ) );
        }
    }


    public ServerContext( DirectoryService service, CoreSession session, Name name ) throws Exception
    {
        this.service = service;
        this.dn = (DN)(DN.fromName( name ).clone());
        this.env = new Hashtable<String, Object>();
        this.env.put( PROVIDER_URL, dn.toString() );
        this.env.put( DirectoryService.JNDI_KEY, service );
        this.session = session;
        OperationManager operationManager = service.getOperationManager();
        
        if ( ! operationManager.hasEntry( new EntryOperationContext( session, ( DN ) dn ) ) )
        {
            throw new NameNotFoundException( I18n.err( I18n.ERR_490, dn ) );
        }
    }


    /**
     * Set the referral handling flag into the operation context using
     * the JNDI value stored into the environment.
     */
    protected void injectReferralControl( OperationContext opCtx )
    {
        if ( "ignore".equalsIgnoreCase( (String)env.get( Context.REFERRAL ) ) )
        {
            opCtx.ignoreReferral();
        }
        else if ( "throw".equalsIgnoreCase( (String)env.get( Context.REFERRAL ) ) )
        {
            opCtx.throwReferral();
        }
        else
        {
            // TODO : handle the 'follow' referral option 
            opCtx.throwReferral();
        }
    }
    // ------------------------------------------------------------------------
    // Protected Methods for Operations
    // ------------------------------------------------------------------------
    // Use these methods instead of manually calling the nexusProxy so we can
    // add request controls to operation contexts before the call and extract 
    // response controls from the contexts after the call.  NOTE that the 
    // JndiUtils.fromJndiControls( requestControls ) must be cleared after each operation.  This makes a 
    // context not thread safe.
    // ------------------------------------------------------------------------

    /**
     * Used to encapsulate [de]marshalling of controls before and after add operations.
     * @param entry
     * @param target
     */
    protected void doAddOperation( DN target, Entry entry ) throws Exception
    {
        // setup the op context and populate with request controls
        AddOperationContext opCtx = new AddOperationContext( session, entry );

        opCtx.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );
        
        // Inject the referral handling into the operation context
        injectReferralControl( opCtx );
        
        // execute add operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.add( opCtx );
    
        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( opCtx.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after delete operations.
     * @param target
     */
    protected void doDeleteOperation( DN target ) throws Exception
    {
        // setup the op context and populate with request controls
        DeleteOperationContext opCtx = new DeleteOperationContext( session, target );

        opCtx.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( opCtx );

        // execute delete operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.delete( opCtx );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( opCtx.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after list operations.
     * @param dn
     * @param aliasDerefMode
     * @param filter
     * @param searchControls
     * @return NamingEnumeration
     */
    protected EntryFilteringCursor doSearchOperation( DN dn, AliasDerefMode aliasDerefMode,
        ExprNode filter, SearchControls searchControls ) throws Exception
    {
        OperationManager operationManager = service.getOperationManager();
        EntryFilteringCursor results = null;
        OperationContext opContext;
        
        Object typesOnlyObj = getEnvironment().get( "java.naming.ldap.typesOnly" );
        boolean typesOnly = false;
        
        if( typesOnlyObj != null )
        {
            typesOnly = Boolean.parseBoolean( typesOnlyObj.toString() );
        }
        
        SearchOperationContext searchContext = null;

        // We have to check if it's a compare operation or a search. 
        // A compare operation has a OBJECT scope search, the filter must
        // be of the form (object=value) (no wildcards), and no attributes
        // should be asked to be returned.
        if ( ( searchControls.getSearchScope() == SearchControls.OBJECT_SCOPE )
            && ( ( searchControls.getReturningAttributes() != null )
                && ( searchControls.getReturningAttributes().length == 0 ) )
            && ( filter instanceof EqualityNode ) )
        {
        	CompareOperationContext compareContext = new CompareOperationContext( session, dn, ((EqualityNode)filter).getAttribute(), ((EqualityNode)filter).getValue() );
            
            // Inject the referral handling into the operation context
            injectReferralControl( compareContext );

            // Call the operation
            boolean result = operationManager.compare( compareContext );

            // setup the op context and populate with request controls
            searchContext = new SearchOperationContext( session, dn, filter,
                searchControls );
            searchContext.setAliasDerefMode( aliasDerefMode );
            searchContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );
            
            searchContext.setTypesOnly(  typesOnly );
            
            if ( result )
            {
                Entry emptyEntry = new DefaultEntry( service.getSchemaManager(), DN.EMPTY_DN ); 
                return new BaseEntryFilteringCursor( new SingletonCursor<Entry>( emptyEntry ), searchContext );
            }
            else
            {
                return new BaseEntryFilteringCursor( new EmptyCursor<Entry>(), searchContext );
            }
        }
        else
        {
            // It's a Search
            
            // setup the op context and populate with request controls
        	searchContext = new SearchOperationContext( session, dn, filter, searchControls );
        	searchContext.setAliasDerefMode( aliasDerefMode );
        	searchContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );
        	searchContext.setTypesOnly(  typesOnly );
            
            // Inject the referral handling into the operation context
            injectReferralControl( searchContext );

            // execute search operation
            results = operationManager.search( searchContext );
        }

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( searchContext.getResponseControls() );

        return results;
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after list operations.
     */
    protected EntryFilteringCursor doListOperation( DN target ) throws Exception
    {
        // setup the op context and populate with request controls
        ListOperationContext listContext = new ListOperationContext( session, target );
        listContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );

        // execute list operation
        OperationManager operationManager = service.getOperationManager();
        EntryFilteringCursor results = operationManager.list( listContext );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( listContext.getResponseControls() );

        return results;
    }


    protected Entry doGetRootDSEOperation( DN target ) throws Exception
    {
        GetRootDSEOperationContext getRootDseContext = new GetRootDSEOperationContext( session, target );
        getRootDseContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );

        // do not reset request controls since this is not an external 
        // operation and not do bother setting the response controls either
        OperationManager operationManager = service.getOperationManager();
        return operationManager.getRootDSE( getRootDseContext );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after lookup operations.
     */
    protected Entry doLookupOperation( DN target ) throws Exception
    {
        // setup the op context and populate with request controls
        // execute lookup/getRootDSE operation
        LookupOperationContext lookupContext = new LookupOperationContext( session, target );
        lookupContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );
        OperationManager operationManager = service.getOperationManager();
        Entry serverEntry = operationManager.lookup( lookupContext );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( lookupContext.getResponseControls() );
        return serverEntry;
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after lookup operations.
     */
    protected Entry doLookupOperation( DN target, String[] attrIds ) throws Exception
    {
        // setup the op context and populate with request controls
        // execute lookup/getRootDSE operation
        LookupOperationContext lookupContext = new LookupOperationContext( session, target, attrIds );
        lookupContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );
        OperationManager operationManager = service.getOperationManager();
        Entry serverEntry = operationManager.lookup( lookupContext );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( lookupContext.getResponseControls() );

        // Now remove the ObjectClass attribute if it has not been requested
        if ( ( lookupContext.getAttrsId() != null ) && ( lookupContext.getAttrsId().size() != 0 ) &&
            ( ( serverEntry.get( SchemaConstants.OBJECT_CLASS_AT ) != null )
                && ( serverEntry.get( SchemaConstants.OBJECT_CLASS_AT ).size() == 0 ) ) )
        {
            serverEntry.removeAttributes( SchemaConstants.OBJECT_CLASS_AT );
        }

        return serverEntry;
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after bind operations.
     */
    protected BindOperationContext doBindOperation( DN bindDn, byte[] credentials, String saslMechanism, 
        String saslAuthId ) throws Exception
    {
        // setup the op context and populate with request controls
        BindOperationContext bindContext = new BindOperationContext( null );
        bindContext.setDn( bindDn );
        bindContext.setCredentials( credentials );
        bindContext.setSaslMechanism( saslMechanism );
        bindContext.setSaslAuthId( saslAuthId );
        bindContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );

        // execute bind operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.bind( bindContext );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( bindContext.getResponseControls() );
        return bindContext;
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after moveAndRename operations.
     */
    protected void doMoveAndRenameOperation( DN oldDn, DN parent, RDN newRdn, boolean delOldDn )
        throws Exception
    {
        // setup the op context and populate with request controls
        MoveAndRenameOperationContext moveAndRenameContext = new MoveAndRenameOperationContext( session, oldDn, parent, new RDN(
            newRdn ), delOldDn );
        moveAndRenameContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( moveAndRenameContext );
        
        // execute moveAndRename operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.moveAndRename( moveAndRenameContext );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( moveAndRenameContext.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after modify operations.
     */
    protected void doModifyOperation( DN dn, List<Modification> modifications ) throws Exception
    {
        // setup the op context and populate with request controls
        ModifyOperationContext modifyContext = new ModifyOperationContext( session, dn, modifications );
        modifyContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( modifyContext );
        
        // execute modify operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.modify( modifyContext );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( modifyContext.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after moveAndRename operations.
     */
    protected void doMove( DN oldDn, DN target ) throws Exception
    {
        // setup the op context and populate with request controls
        MoveOperationContext moveContext = new MoveOperationContext( session, oldDn, target );
        moveContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( moveContext );
        
        // execute move operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.move( moveContext );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( moveContext.getResponseControls() );
    }


    /**
     * Used to encapsulate [de]marshalling of controls before and after rename operations.
     */
    protected void doRename( DN oldDn, RDN newRdn, boolean delOldRdn ) throws Exception
    {
        // setup the op context and populate with request controls
        RenameOperationContext renameContext = new RenameOperationContext( session, oldDn, newRdn, delOldRdn );
        renameContext.addRequestControls( JndiUtils.fromJndiControls( requestControls ) );

        // Inject the referral handling into the operation context
        injectReferralControl( renameContext );
        
        // execute rename operation
        OperationManager operationManager = service.getOperationManager();
        operationManager.rename( renameContext );

        // clear the request controls and set the response controls 
        requestControls = EMPTY_CONTROLS;
        responseControls = JndiUtils.toJndiControls( renameContext.getResponseControls() );
    }

    
    public CoreSession getSession()
    {
        return session;
    }
    
    
    public DirectoryService getDirectoryService()
    {
        return service;
    }
    
    
    // ------------------------------------------------------------------------
    // New Impl Specific Public Methods
    // ------------------------------------------------------------------------

    /**
     * Gets a handle on the root context of the DIT.  The RootDSE as the present user.
     *
     * @return the rootDSE context
     * @throws NamingException if this fails
     */
    public abstract ServerContext getRootContext() throws NamingException;


    /**
     * Gets the {@link DirectoryService} associated with this context.
     *
     * @return the directory service associated with this context
     */
    public DirectoryService getService()
    {
        return service;
    }


    // ------------------------------------------------------------------------
    // Protected Accessor Methods
    // ------------------------------------------------------------------------

    
    /**
     * Gets the distinguished name of the entry associated with this Context.
     * 
     * @return the distinguished name of this Context's entry.
     */
    protected DN getDn()
    {
        return dn;
    }


    // ------------------------------------------------------------------------
    // JNDI Context Interface Methods
    // ------------------------------------------------------------------------

    /**
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException
    {
        for ( DirectoryListener listener : listeners.values() )
        {
            try
            {
                service.getEventService().removeListener( listener );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        
        listeners.clear();
    }


    /**
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException
    {
        return dn.getName();
    }


    /**
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable<String, Object> getEnvironment()
    {
        return env;
    }


    /**
     * @see javax.naming.Context#addToEnvironment(java.lang.String, 
     * java.lang.Object)
     */
    public Object addToEnvironment( String propName, Object propVal ) throws NamingException
    {
        return env.put( propName, propVal );
    }


    /**
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment( String propName ) throws NamingException
    {
        return env.remove( propName );
    }


    /**
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext( String name ) throws NamingException
    {
        return createSubcontext( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext( Name name ) throws NamingException
    {
        DN target = buildTarget( DN.fromName( name ) );
        Entry serverEntry = null;
        
        try
        {
            serverEntry = service.newEntry( target );
        }
        catch ( LdapException le )
        {
            throw new NamingException( le.getMessage() );
        }
        
        try
        {
            serverEntry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, JavaLdapSupport.JCONTAINER_ATTR );
        }
        catch ( LdapException le )
        {
            throw new SchemaViolationException( I18n.err( I18n.ERR_491, name) );
        }

        // Now add the CN attribute, which is mandatory
        RDN rdn = target.getRdn();

        if ( rdn != null )
        {
            if ( SchemaConstants.CN_AT.equals( rdn.getNormType() ) )
            {
                serverEntry.put( rdn.getUpType(), rdn.getUpValue() );
            }
            else
            {
                // No CN in the rdn, this is an error
                throw new SchemaViolationException( I18n.err( I18n.ERR_491, name) );
            }
        }
        else
        {
            // No CN in the rdn, this is an error
            throw new SchemaViolationException( I18n.err( I18n.ERR_491, name) );
        }

        /*
         * Add the new context to the server which as a side effect adds 
         * operational attributes to the serverEntry refering instance which
         * can them be used to initialize a new ServerLdapContext.  Remember
         * we need to copy over the controls as well to propagate the complete 
         * environment besides what's in the hashtable for env.
         */
        try
        {
            doAddOperation( target, serverEntry );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
        
        ServerLdapContext ctx = null;
        
        try
        {
            ctx = new ServerLdapContext( service, session.getEffectivePrincipal(), DN.toName( target ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
        
        return ctx;
    }


    /**
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext( String name ) throws NamingException
    {
        destroySubcontext( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext( Name name ) throws NamingException
    {
        DN target = buildTarget( DN.fromName( name ) );

        if ( target.size() == 0 )
        {
            throw new NoPermissionException( I18n.err( I18n.ERR_492 ) );
        }

        try
        {
            doDeleteOperation( target );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    /**
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     */
    public void bind( String name, Object obj ) throws NamingException
    {
        bind( new LdapName( name ), obj );
    }


    private void injectRdnAttributeValues( DN target, Entry serverEntry ) throws NamingException
    {
        // Add all the RDN attributes and their values to this entry
        RDN rdn = target.getRdn( target.size() - 1 );

        if ( rdn.size() == 1 )
        {
            serverEntry.put( rdn.getUpType(), rdn.getUpValue() );
        }
        else
        {
            for ( AVA atav : rdn )
            {
                serverEntry.put( atav.getUpType(), atav.getUpValue() );
            }
        }
    }


    /**
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind( Name name, Object obj ) throws NamingException
    {
        // First, use state factories to do a transformation
        DirStateFactory.Result res = DirectoryManager.getStateToBind( obj, name, this, env, null );

        DN target = buildTarget( DN.fromName( name ) );

        // let's be sure that the Attributes is case insensitive
        Entry outServerEntry = null;
        
        try
        {
            outServerEntry = ServerEntryUtils.toServerEntry( AttributeUtils.toCaseInsensitive( res
                .getAttributes() ), target, service.getSchemaManager() );
        }
        catch ( LdapInvalidAttributeTypeException liate )
        {
            throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
        }

        if ( outServerEntry != null )
        {
            try
            {
                doAddOperation( target, outServerEntry );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
            return;
        }

        if ( obj instanceof Entry )
        {
            try
            {
                doAddOperation( target, ( Entry ) obj );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        // Check for Referenceable
        else if ( obj instanceof Referenceable )
        {
            throw new NamingException( I18n.err( I18n.ERR_493 ) );
        }
        // Store different formats
        else if ( obj instanceof Reference )
        {
            // Store as ref and add outAttrs
            throw new NamingException( I18n.err( I18n.ERR_494 ) );
        }
        else if ( obj instanceof Serializable )
        {
            // Serialize and add outAttrs
            Entry serverEntry = null;
            
            try
            {
                serverEntry = service.newEntry( target );
            }
            catch ( LdapException le )
            {
                throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
            }

            if ( ( outServerEntry != null ) && ( outServerEntry.size() > 0 ) )
            {
                for ( EntryAttribute serverAttribute : outServerEntry )
                {
                    try
                    {
                        serverEntry.put( serverAttribute );
                    }
                    catch ( LdapException le )
                    {
                        throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
                    }
                }
            }

            // Get target and inject all rdn attributes into entry
            injectRdnAttributeValues( target, serverEntry );

            // Serialize object into entry attributes and add it.
            try
            { 
                JavaLdapSupport.serialize( serverEntry, obj, service.getSchemaManager() );
            }
            catch ( LdapException le )
            {
                throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
            }
            
            try
            {
                doAddOperation( target, serverEntry );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        else if ( obj instanceof DirContext )
        {
            // Grab attributes and merge with outAttrs
            Entry serverEntry = null;
            
            try
            { 
                serverEntry = ServerEntryUtils.toServerEntry( ( ( DirContext ) obj ).getAttributes( "" ),
                    target, service.getSchemaManager() );
            }
            catch ( LdapInvalidAttributeTypeException liate )
            {
                throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
            }

            if ( ( outServerEntry != null ) && ( outServerEntry.size() > 0 ) )
            {
                for ( EntryAttribute serverAttribute : outServerEntry )
                {
                    try
                    {                 
                        serverEntry.put( serverAttribute );
                    }
                    catch ( LdapException le )
                    {
                        throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
                    }
                }
            }

            injectRdnAttributeValues( target, serverEntry );
            try
            {
                doAddOperation( target, serverEntry );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        else
        {
            throw new NamingException( I18n.err( I18n.ERR_495, obj ) );
        }
    }


    /**
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename( String oldName, String newName ) throws NamingException
    {
        rename( new LdapName( oldName ), new LdapName( newName ) );
    }


    /**
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename( Name oldName, Name newName ) throws NamingException
    {
        DN oldDn = buildTarget( DN.fromName( oldName ) );
        DN newDn = buildTarget( DN.fromName( newName ) );

        if ( oldDn.size() == 0 )
        {
            throw new NoPermissionException( I18n.err( I18n.ERR_312 ) );
        }

        // calculate parents
        DN oldParent = (DN)oldDn.clone();
        
        try
        {
            oldParent.remove( oldDn.size() - 1 );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new NamingException( I18n.err( I18n.ERR_313, lide.getMessage() ) );
        }
        
        DN newParent = ( DN ) newDn.clone();
        
        try
        {
            newParent.remove( newDn.size() - 1 );
        }
        catch ( LdapInvalidDnException lide )
        {
            throw new NamingException( I18n.err( I18n.ERR_313, lide.getMessage() ) );
        }


        RDN oldRdn = oldDn.getRdn();
        RDN newRdn = newDn.getRdn();
        boolean delOldRdn = true;

        /*
         * Attempt to use the java.naming.ldap.deleteRDN environment property
         * to get an override for the deleteOldRdn option to modifyRdn.  
         */
        if ( null != env.get( DELETE_OLD_RDN_PROP ) )
        {
            String delOldRdnStr = ( String ) env.get( DELETE_OLD_RDN_PROP );
            delOldRdn = !delOldRdnStr.equalsIgnoreCase( "false" ) && !delOldRdnStr.equalsIgnoreCase( "no" )
                && !delOldRdnStr.equals( "0" );
        }

        /*
         * We need to determine if this rename operation corresponds to a simple
         * RDN name change or a move operation.  If the two names are the same
         * except for the RDN then it is a simple modifyRdn operation.  If the
         * names differ in size or have a different baseDN then the operation is
         * a move operation.  Furthermore if the RDN in the move operation 
         * changes it is both an RDN change and a move operation.
         */
        if ( oldParent.equals( newParent ) )
        {
            try
            {
                doRename( oldDn, newRdn, delOldRdn );
            }
            catch ( Exception e )
            {
                JndiUtils.wrap( e );
            }
        }
        else
        {
            if ( newRdn.equals( oldRdn ) )
            {
                try
                {
                    doMove( oldDn, newParent );
                }
                catch ( Exception e )
                {
                    JndiUtils.wrap( e );
                }
            }
            else
            {
                try
                {
                    doMoveAndRenameOperation( oldDn, newParent, newRdn, delOldRdn );
                }
                catch ( Exception e )
                {
                    JndiUtils.wrap( e );
                }
            }
        }
    }


    /**
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind( String name, Object obj ) throws NamingException
    {
        rebind( new LdapName( name ), obj );
    }


    /**
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind( Name name, Object obj ) throws NamingException
    {
        DN target = buildTarget( DN.fromName( name ) );
        OperationManager operationManager = service.getOperationManager();
        
        try
        {
            if ( operationManager.hasEntry( new EntryOperationContext( session, target ) ) )
            {
                doDeleteOperation( target );
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        bind( name, obj );
    }


    /**
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind( String name ) throws NamingException
    {
        unbind( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind( Name name ) throws NamingException
    {
        try
        {
            doDeleteOperation( buildTarget( DN.fromName( name ) ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    /**
     * @see javax.naming.Context#lookup(java.lang.String)
     */
    public Object lookup( String name ) throws NamingException
    {
        if ( StringTools.isEmpty( name ) )
        {
            return lookup( new LdapName( "" ) );
        }
        else
        {
            return lookup( new LdapName( name ) );
        }
    }


    /**
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup( Name name ) throws NamingException
    {
        Object obj;
        DN target = buildTarget( DN.fromName( name ) );

        Entry serverEntry = null;

        try
        {
            if ( name.size() == 0 )
            {
                serverEntry = doGetRootDSEOperation( target );
            }
            else
            {
                serverEntry = doLookupOperation( target );
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }

        try
        {
            obj = DirectoryManager.getObjectInstance( null, name, this, env, 
                ServerEntryUtils.toBasicAttributes( serverEntry ) );
        }
        catch ( Exception e )
        {
            String msg = I18n.err( I18n.ERR_497, target );
            NamingException ne = new NamingException( msg );
            ne.setRootCause( e );
            throw ne;
        }

        if ( obj != null )
        {
            return obj;
        }

        // First lets test and see if the entry is a serialized java object
        if ( serverEntry.get( JavaLdapSupport.JCLASSNAME_ATTR ) != null )
        {
            // Give back serialized object and not a context
            return JavaLdapSupport.deserialize( serverEntry );
        }

        // Initialize and return a context since the entry is not a java object
        ServerLdapContext ctx = null;
        
        try
        {
            ctx = new ServerLdapContext( service, session.getEffectivePrincipal(), DN.toName( target ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
        
        return ctx;
    }


    /**
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink( String name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink( Name name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Non-federated implementation presuming the name argument is not a 
     * composite name spanning multiple namespaces but a compound name in 
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String. 
     * 
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser( String name ) throws NamingException
    {
        return new NameParser()
        {
            public Name parse( String name ) throws NamingException
            {
                try
                {
                    return DN.toName( new DN( name ) );
                }
                catch ( LdapInvalidDnException lide )
                {
                    throw new InvalidNameException( lide.getMessage() );
                }
            }
        };
    }


    /**
     * Non-federated implementation presuming the name argument is not a 
     * composite name spanning multiple namespaces but a compound name in 
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String Name.
     * 
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser( final Name name ) throws NamingException
    {
        return new NameParser()
        {
            public Name parse( String n ) throws NamingException
            {
                try
                {
                    return DN.toName( new DN( name.toString() ) );
                }
                catch ( LdapInvalidDnException lide )
                {
                    throw new InvalidNameException( lide.getMessage() );
                }
            }
        };
    }


    /**
     * @see javax.naming.Context#list(java.lang.String)
     */
    @SuppressWarnings(value =
        { "unchecked" })
    public NamingEnumeration list( String name ) throws NamingException
    {
        return list( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    @SuppressWarnings(value =
        { "unchecked" })
    public NamingEnumeration list( Name name ) throws NamingException
    {
        try
        {
            return new NamingEnumerationAdapter( doListOperation( buildTarget( DN.fromName( name ) ) ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
            return null; // shut up compiler
        }
    }


    /**
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    @SuppressWarnings(value =
        { "unchecked" })
    public NamingEnumeration listBindings( String name ) throws NamingException
    {
        return listBindings( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    @SuppressWarnings(value =
        { "unchecked" })
    public NamingEnumeration listBindings( Name name ) throws NamingException
    {
        // Conduct a special one level search at base for all objects
        DN base = buildTarget( DN.fromName( name ) );
        PresenceNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        AliasDerefMode aliasDerefMode = AliasDerefMode.getEnum( getEnvironment() );
        try
        {
            return new NamingEnumerationAdapter( doSearchOperation( base, aliasDerefMode, filter, ctls ) );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
            return null; // shutup compiler
        }
    }


    /**
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName( String name, String prefix ) throws NamingException
    {
        return composeName( new LdapName( name ), new LdapName( prefix ) ).toString();
    }


    /**
     * @see javax.naming.Context#composeName(javax.naming.Name,
     * javax.naming.Name)
     */
    public Name composeName( Name name, Name prefix ) throws NamingException
    {
        // No prefix reduces to name, or the name relative to this context
        if ( prefix == null || prefix.size() == 0 )
        {
            return name;
        }

        /*
         * Example: This context is ou=people and say name is the relative
         * name of uid=jwalker and the prefix is dc=domain.  Then we must
         * compose the name relative to prefix which would be:
         * 
         * uid=jwalker,ou=people,dc=domain.
         * 
         * The following general algorithm generates the right name:
         *      1). Find the Dn for name and walk it from the head to tail
         *          trying to match for the head of prefix.
         *      2). Remove name components from the Dn until a match for the 
         *          head of the prefix is found.
         *      3). Return the remainder of the fqn or Dn after chewing off some
         */

        // 1). Find the Dn for name and walk it from the head to tail
        DN fqn = buildTarget( DN.fromName( name ) );
        String head = prefix.get( 0 );

        // 2). Walk the fqn trying to match for the head of the prefix
        while ( fqn.size() > 0 )
        {
            // match found end loop
            if ( fqn.get( 0 ).equalsIgnoreCase( head ) )
            {
                return DN.toName( fqn );
            }
            else
            // 2). Remove name components from the Dn until a match 
            {
                try
                {
                    fqn.remove( 0 );
                }
                catch ( LdapInvalidDnException lide )
                {
                    throw new NamingException( lide.getMessage() );
                }
            }
        }

        String msg = I18n.err( I18n.ERR_498, prefix, dn );
        throw new NamingException( msg );
    }


    // ------------------------------------------------------------------------
    // EventContext implementations
    // ------------------------------------------------------------------------

    public void addNamingListener( Name name, int scope, NamingListener namingListener ) throws NamingException
    {
        ExprNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );

        try
        {
            DirectoryListener listener = new EventListenerAdapter( ( ServerLdapContext ) this, namingListener );
            NotificationCriteria criteria = new NotificationCriteria();
            criteria.setFilter( filter );
            criteria.setScope( SearchScope.getSearchScope( scope ) );
            criteria.setAliasDerefMode( AliasDerefMode.getEnum( env ) );
            criteria.setBase( buildTarget( DN.fromName( name ) ) );
            
            service.getEventService().addListener( listener );
            listeners.put( namingListener, listener );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    public void addNamingListener( String name, int scope, NamingListener namingListener ) throws NamingException
    {
        addNamingListener( new LdapName( name ), scope, namingListener );
    }


    public void removeNamingListener( NamingListener namingListener ) throws NamingException
    {
        try
        {
            DirectoryListener listener = listeners.remove( namingListener );
            
            if ( listener != null )
            {
                service.getEventService().removeListener( listener );
            }
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
    }


    public boolean targetMustExist() throws NamingException
    {
        return false;
    }


    /**
     * Allows subclasses to register and unregister listeners.
     *
     * @return the set of listeners used for tracking registered name listeners.
     */
    protected Map<NamingListener, DirectoryListener> getListeners()
    {
        return listeners;
    }


    // ------------------------------------------------------------------------
    // Utility Methods to Reduce Code
    // ------------------------------------------------------------------------

    /**
     * Clones this context's DN and adds the components of the name relative to 
     * this context to the left hand side of this context's cloned DN. 
     * 
     * @param relativeName a name relative to this context.
     * @return the name of the target
     * @throws InvalidNameException if relativeName is not a valid name in
     *      the LDAP namespace.
     */
    DN buildTarget( DN relativeName ) throws NamingException
    {
        DN target = ( DN ) dn.clone();

        // Add to left hand side of cloned DN the relative name arg
        try
        {
            target.addAllNormalized( target.size(), relativeName );
        }
        catch (LdapInvalidDnException lide )
        {
            throw new InvalidNameException( lide.getMessage() );
        }
        
        return target;
    }
}

