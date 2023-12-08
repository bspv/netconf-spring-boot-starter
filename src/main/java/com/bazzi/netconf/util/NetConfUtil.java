package com.bazzi.netconf.util;

import com.bazzi.netconf.entity.SshConf;
import com.cisco.stbarth.netconf.anc.NetconfException;
import com.cisco.stbarth.netconf.anc.NetconfSSHClient;
import com.cisco.stbarth.netconf.anc.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class NetConfUtil {
    private static final Logger logger = LoggerFactory.getLogger(NetConfUtil.class);

    public static NetconfSSHClient createClient(SshConf sshConf) {
        try {
            if (sshConf == null)
                return null;
            NetconfSSHClient netconfSSHClient = new NetconfSSHClient(sshConf.getHost(), sshConf.getPort(), sshConf.getUsername());
            netconfSSHClient.setPassword(sshConf.getPassword());
            netconfSSHClient.setStrictHostKeyChecking(false);
            netconfSSHClient.setTimeout(3600000);
            netconfSSHClient.setKeepalive(15000);

            return netconfSSHClient;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static NetconfSession createSession(SshConf sshConf) {
        try {
            if (sshConf == null)
                return null;
            NetconfSSHClient client = createClient(sshConf);
            if (client == null)
                return null;
            return client.createSession();
        } catch (NetconfException.ProtocolException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
