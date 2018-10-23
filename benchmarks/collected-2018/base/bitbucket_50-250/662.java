// https://searchcode.com/api/result/55057174/

// Copyright (c) 2010 Jeffrey D. Brennan
// 
// License: http://www.opensource.org/licenses/mit-license.php

package rb;

import rainbow.Console;
import rainbow.Nil;
import rainbow.functions.Environment;
import rainbow.functions.eval.Apply;
import rainbow.parser.ArcParser;
import rainbow.parser.ParseException;
import rainbow.types.ArcObject;
import rainbow.types.Hash;
import rainbow.types.JavaObject;
import rainbow.types.Output;
import rainbow.types.Pair;
import rainbow.types.Symbol;
import rainbow.vm.Instruction;
import rainbow.vm.VM;
import rainbow.vm.interpreter.visitor.Visitor;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;

public class Servlet extends HttpServlet
{
    private VM _vm = new VM(); // Do I need to share this between GET requests?

    public void init()
        throws ServletException
    {
        try {
            String uri = getInitParameter("init.uri");
            if (uri == null) uri = "/webapp.arc";
            System.out.println("init " + uri);
            ServletContext context = getServletContext();
            InputStream in = context.getResourceAsStream(uri);
            if (in == null) {
                throw new ServletException("Initialization not found");
            }
            initRainbow();
            (Symbol.mkSym("*context*")).setValue(JavaObject.wrap(context));
            load(_vm, uri, in);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServletException("Initialization failure");
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
        if (request.getParameter("reload") != null) init(); // For rapid testing

        response.setContentType("text/html");
        ServletOutputStream out = response.getOutputStream();
        try {
            String uri = request.getRequestURI();
            if (uri.equals("/")) {
                uri = getInitParameter("welcome.uri");
                if (uri == null) uri = "/index";
            }
            System.out.println("GET " + uri);
            Symbol sym = Symbol.mkSym(uri);
            if (!sym.bound()) {
                response.sendError(response.SC_NOT_FOUND, uri.toString());
                return;
            }
            ArcObject fn = sym.value();

            // set stdIn to NIL?
            PrintStream stream = new PrintStream(out);
            rainbow.functions.IO.stdOut.set(new Output(stream));
            Pair args = new Pair(JavaObject.wrap(request),
                                 new Pair(JavaObject.wrap(response), Nil.NIL));
            new Apply().invokef(_vm, fn, args);
            stream.flush();
            stream.close();
        } catch (ThreadDeath ex) {
            throw ex; // Shouldn't try to handle this.
        } catch (Throwable ex) {
            ex.printStackTrace();
            // Get the root stack trace
            while (ex.getCause() != null) ex = ex.getCause();
            out.print("Exception: " + ex);
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }

    private void initRainbow()
        throws Exception
    {
        if (!Symbol.mkSym("sig").bound()) {
            Environment.init();
            (Symbol.mkSym("*env*")).setValue(new Hash()); // Is this required?
            (Symbol.mkSym("call*")).setValue(new Hash());
            (Symbol.mkSym("sig")).setValue(new Hash());
            for (String file : new String[]{"arc",
                                            "strings",
                                            "lib/bag-of-tricks",
                                            "rainbow/rainbow"}) {
                // loadFile won't work in Tomcat since "." is not
                // the root of the war file in Tomcat.
                // But it will work in the App Engine dev server.
                // Not sure if it will work in the real App Engine (yet!)
                Console.loadFile(_vm, new String[]{"lib"}, file);
            }
        }
    }

    // private methods copied from rainbow.Console
  private static void load(VM vm, String name, InputStream stream) throws ParseException {
    ArcParser parser = new ArcParser(stream);
    ArcObject expression = parser.parseOneLine();
    while (expression != null) {
      compileAndEval(vm, expression);
      expression = parser.parseOneLine();
    }
  }

  private static Visitor mkVisitor(final ArcObject owner) {
    return new Visitor() {
      public void accept(Instruction o) {
        o.belongsTo(owner);
      }
    };
  }

  private static ArcObject compileAndEval(VM vm, ArcObject expression) {
    expression = rainbow.vm.compiler.Compiler.compile(vm, expression, new Map[0]).reduce();
    List i = new ArrayList();
    expression.addInstructions(i);
    Pair instructions = Pair.buildFrom(i);
    instructions.visit(mkVisitor(expression));
    vm.pushInvocation(null, instructions);
    return vm.thread();
  }


}

