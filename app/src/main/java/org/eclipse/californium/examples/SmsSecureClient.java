package org.eclipse.californium.examples;

import android.provider.Telephony;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.scandium.SMSDTLSConnector;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.logging.Level;

import cn.sms.util.SmsHander;
import cn.sms.util.SmsSocket;
import cn.sms.util.SmsSocketAddress;


public class SmsSecureClient {

	static {
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.FINE);
	}


	private static final String TRUST_STORE_PASSWORD = "rootPass";
	private final static String KEY_STORE_PASSWORD = "endPass";
	private static final String KEY_STORE_LOCATION = "assets/certs/keyStore.pfx";
	private static final String TRUST_STORE_LOCATION = "assets/certs/trustStore.pfx";

	private SMSDTLSConnector smsDtlsConnector;
	private SmsSocket smsSocket;

	public SmsSecureClient(SmsSocket smsSocket, SmsSocketAddress smsSocketAddress) {
		this.smsSocket = smsSocket;

		try {
			// load key store
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			InputStream in = getClass().getClassLoader().getResourceAsStream(KEY_STORE_LOCATION);
			//InputStream in2 = getClass().getResourceAsStream("/" + KEY_STORE_LOCATION);
			keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());

			// load trust store
			KeyStore trustStore = KeyStore.getInstance("PKCS12");
			InputStream inTrust = getClass().getClassLoader().getResourceAsStream(TRUST_STORE_LOCATION);
			trustStore.load(inTrust, TRUST_STORE_PASSWORD.toCharArray());

			// You can load multiple certificates if needed
			Certificate[] trustedCertificates = new Certificate[1];
			trustedCertificates[0] = trustStore.getCertificate("root");

			DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
			builder.setPskStore(new StaticPskStore("Client_identity", "secretPSK".getBytes()));
			builder.setIdentity((PrivateKey) keyStore.getKey("client", KEY_STORE_PASSWORD.toCharArray()),
					keyStore.getCertificateChain("client"), true);
			builder.setTrustStore(trustedCertificates);
			smsDtlsConnector = new SMSDTLSConnector(builder.build(), smsSocket, smsSocketAddress);
			smsDtlsConnector.setRawDataReceiver(new RawDataChannel() {
				@Override
				public void receiveData(RawData raw) {
					System.out.println(raw.getBytes());
				}
			});

		} catch (GeneralSecurityException | IOException e) {
			System.err.println("Could not load the keystore");
			e.printStackTrace();
		}
	}

	public void test(String u) {

		Request req = new Request(CoAP.Code.GET);
		req.setURI(u);
		req.getDestinationPort();
		req.getDestination();
		final InetSocketAddress peer = new InetSocketAddress(req.getDestination(), req.getDestinationPort());

		smsSocket.setReceiverHander(new SmsHander() {
			@Override
			public void smsHandle(byte[] message) {
				smsDtlsConnector.receive(new RawData(message, peer));
			}
		});
		try {

			smsDtlsConnector.start();
			smsDtlsConnector.send(new RawData("Hello World".getBytes(),peer));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
