package com.riil.build;

import java.io.File;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * User: wangchongyang on 2017/9/7 0007.
 */
public class SVNUtils {

    private static final String URL_REPOSITORY = "https://172.17.189.19/svn/RIIL_WebFramework/trunk/source/";
    private static final String CHECKOUT_DIRECTORY = "F:/RIIL_WebFramework/trunk/source";
    private static final String USERNAME = "wangchongyang";
    private static final String PASSWORD = "wchy";

    public static void main(String[] args) throws SVNException {
//        根据访问协议初始化工厂
        DAVRepositoryFactory.setup();
//        初始化仓库
        final SVNURL url = SVNURL.parseURIEncoded(URL_REPOSITORY);
        final SVNRepository repository = SVNRepositoryFactory.create(url);
        final ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(USERNAME, PASSWORD.toCharArray());
        repository.setAuthenticationManager(authManager);
        System.out.println(repository.getRepositoryRoot(true));
        final SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), authManager);
        final long nowRevision = clientManager.getUpdateClient().doCheckout(url, new File(CHECKOUT_DIRECTORY), SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
        System.out.println("checkout OK!now revision is " + nowRevision);

    }
}
