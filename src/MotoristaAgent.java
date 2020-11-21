import java.util.Hashtable;

import jade.core.*; 
import jade.core.behaviours.*; 
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.ServiceDescription; 
import jade.domain.FIPAAgentManagement.DFAgentDescription; 
import jade.domain.DFService; 
import jade.domain.FIPAException;

public class MotoristaAgent extends Agent {
	
	private static final long serialVersionUID = 3211228974125503848L;

	// O localDeAtuacao corresponde as cidades para onde o motorista aceita levar o passageiro
	private Hashtable localDeAtuacao;

	// motoristaGui corresponde a interface gráfica onde pode-se adicionar os locais de atuação do motorista
	private MotoristaGui motoristaGui;

	// Inicializa o agente
	protected void setup() {
		// Cria o  localDeAtuacao
		localDeAtuacao = new Hashtable();

		// Cria e mostra a interface gráfica
		motoristaGui = new MotoristaGui(this);
		motoristaGui.showGui();

		// Registra o serviço de taxi nas páginas amarelas
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("realizar-viagem");
		sd.setName("JADE-uber");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Adiciona o comportamento de oferecer corridas
		addBehaviour(new OfferRequestsServer());

		// Adiciona o comportamento de realizar a corrida 
		addBehaviour(new PurchaseOrdersServer());
	}

	// Operações de eliminar o agente
	protected void takeDown() {
		// Remove o agente das páginas amarelas
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Fecha a interface gráfica
		motoristaGui.dispose();
		// Imprime mensagem de finalização
		System.out.println("Motorista "+getAID().getName().split("@")[0]+" pronto.");
	}

	/**
     É realizado pela interface quando o usuário adiciona novos locais de atuação
	 */
	public void atualizarLocaisDeAtuacao(final String local, final int preco) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				localDeAtuacao.put(local, new Integer(preco));
				System.out.println("Viagem para " + local+". Preco = "+preco);
			}
		} );
	}

	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// Recebeu a mensagem CFP. Processa ela
				String local = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer preco = (Integer) localDeAtuacao.get(local);
				if (preco != null) {
					// A corrida está pronta para ser realizada, responde com o preço.
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(preco.intValue()));
				}
				else {
					// A corrida não está pronta para ser realizada
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // Fim da classe interna OfferRequestsServer

	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// Recebeu mensagem de aceitação. Processa ela
				String local = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer preco = (Integer) localDeAtuacao.remove(local);
				if (preco != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println("Viagem para " +local+" realizada. Passageiro "+msg.getSender().getName().split("@")[0]);
				}
				else {
					// A corrida foi realizada por outro motorista neste intervalo de tempo.
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // Fim da classe interna OfferRequestsServer
}
