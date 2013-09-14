//
//  HttpServer
//  Copyright 2011 by Florian Richter
//  First released 13.04.2011 at http://yacy.net
//  
//  $LastChangedDate$
//  $LastChangedRevision$
//  $LastChangedBy$
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//  
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//  
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program in the file lgpl21.txt
//  If not, see <http://www.gnu.org/licenses/>.
//

package net.yacy.http;

import java.net.InetSocketAddress;
import java.net.SocketException;
import net.yacy.cora.util.ConcurrentLog;
import net.yacy.search.Switchboard;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

/**
 * class to embedded jetty http server into YaCy
 */
public class HttpServer {

    private Server server;

    /**
     * @param port TCP Port to listen for http requests
     */
    public HttpServer(int port) {
        Switchboard sb = Switchboard.getSwitchboard();
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setName("httpd:"+Integer.toString(port));
        //connector.setThreadPool(new QueuedThreadPool(20));
        server.addConnector(connector);
       
        YacyDomainHandler domainHandler = new YacyDomainHandler();
        domainHandler.setAlternativeResolver(sb.peers);

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{"index.html"});
        resource_handler.setResourceBase("htroot/");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]
           {domainHandler, new ProxyCacheHandler(), new ProxyHandler(),
            new RewriteHandler(), new SSIHandler(new TemplateHandler()),
            resource_handler, new DefaultHandler()});

        YaCySecurityHandler securityHandler = new YaCySecurityHandler();
        securityHandler.setLoginService(new YaCyLoginService());
        securityHandler.setRealmName("YaCy Admin Interface");
        securityHandler.setHandler(new CrashProtectionHandler(handlers));

        // context handler for dispatcher and security
        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
        context.setHandler(securityHandler);

        server.setHandler(context);
    }

    /**
     * start http server
     */
    public void start() throws Exception {
        server.start();
    }

    /**
     * stop http server and wait for it
     */
    public void stop() throws Exception {
        server.stop();
        server.join();
    }

    public void setMaxSessionCount(int maxBusy) {
        // TODO:
    }

    public boolean withSSL() {
        return false; // TODO:
    }

    public void reconnect(int milsec) {
        try {
            Thread.sleep(milsec);
        } catch (final InterruptedException e) {
            ConcurrentLog.logException(e);
        } catch (final Exception e) {
            ConcurrentLog.logException(e);
        }
        try {
            server.stop();
            server.join();
            server.start();
        } catch (Exception ex) {
            ConcurrentLog.logException(ex);
        }
    }

    public InetSocketAddress generateSocketAddress(String port) throws SocketException {
        return null; // TODO:
    }

    public int getMaxSessionCount() {
        return server.getThreadPool().getThreads();
    }

    public int getJobCount() {
        return getMaxSessionCount() - server.getThreadPool().getIdleThreads(); // TODO:
    }

}
