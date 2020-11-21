import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.gui.DFAgentDscDlg;

public class PassageiroAgent extends Agent {

	private static final long serialVersionUID = -5028747553588389618L;

	// localDeDestino corresponde para onde o passageiro deseja ir
	private String localDeDestino;
	// motoristasAgents corresponde aos motoristas presentes no ponto de taxi
	private AID[] motoristasAgents;

	// Inicializa o agente do passageiro
	protected void setup() {
		// Mostra passageiro pronto para realizar viagem
		System.out.println(getAID().getName().split("@")[0] + " esta pronto para realizar viagem.");

		// Obtém locais de destino dos motoristas disponíveis
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			localDeDestino = (String) args[0];
			System.out.println("Quero ir para "+localDeDestino);

			// Adiciona comportamento de requisitar uma corrida
			addBehaviour(new TickerBehaviour(this, 10000) {
				
				private static final long serialVersionUID = 4003448980930527834L;

				protected void onTick() {
					System.out.println("Quero ir para "+localDeDestino);
					// Atualiza lista de motoristas disponíveis
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("realizar-viagem");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Motoristas encontrados:");
						motoristasAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							motoristasAgents[i] = result[i].getName();
							System.out.println(motoristasAgents[i].getName().split("@")[0]);
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Executa requisição
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}
		else {
			// Mata requisição por falta de dados
			System.out.println("Destino nao informado");
			doDelete();
		}
	}

	// Retira o passageiro da lista de passageiros disponíveis
	protected void takeDown() {
		System.out.println("Passageiro "+getAID().getName().split("@")[0]+" realizou viagem.");
	}

	/**
	 * Comportamento do passageiro
	 */
	private class RequestPerformer extends Behaviour {

		private static final long serialVersionUID = -1254793636787752644L;

		private AID motoristaProximo; // motorista mais próximo 
		private int menorPreco;  // motorista que oferece o menor preço de viagem
		private int repliesCnt = 0; // Contador de motoristas com mesmo destino
		private MessageTemplate mt; // Template de motoristas com mesmo destino
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				// Mensagem cfp
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < motoristasAgents.length; ++i) {
					cfp.addReceiver(motoristasAgents[i]);
				} 
				cfp.setContent(localDeDestino);
				cfp.setConversationId("corrida");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Valor único
				myAgent.send(cfp);
				// Prepara o templeta para receber propostas
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("corrida"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Recebe propostas de todos os motoristas
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Repete requisições
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// Oferta
						int distancia = Integer.parseInt(reply.getContent());
						if (motoristaProximo == null || distancia < menorPreco) {
							// Melhor oferta atual
							menorPreco = distancia;
							motoristaProximo = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= motoristasAgents.length) {
						// Recebeu todas as requisições
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Pede a corrida para o motorista com o menor preço
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(motoristaProximo);
				order.setContent(localDeDestino);
				order.setConversationId("corrida");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepara o template para receber o pedido de corrida
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("corrida"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				// Recebe o pedido de corrida
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Pede corrida
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Corrida aceita, motorista parte para a viagem e sai do ponto de taxi
						System.out.println("Motorista " +reply.getSender().getName().split("@")[0] +" encontrado para " + localDeDestino);
						System.out.println("Preco = "+ menorPreco);
						myAgent.doDelete();
					}
					else {
						System.out.println("Nenhum taxi disponivel");
					}

					step = 4;
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			if (step == 2 && motoristaProximo == null) {
				System.out.println("Nenhum taxi disponivel para esse local");
			}
			return ((step == 2 && motoristaProximo == null) || step == 4);
		}
	} 
}