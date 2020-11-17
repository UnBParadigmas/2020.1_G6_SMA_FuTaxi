import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDeion;
import jade.domain.FIPAAgentManagement.ServiceDeion;
import jade.gui.DFAgentDscDlg;

public class PassageiroAgent extends Agent {
	// The title of the book to buy
	private String localDeDestino;
	// The list of known seller agents
	private AID[] motoristasAgents;
	
	private PassageiroGui passageiroGui;

	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println(getAID().getName()+" está pronto para realizar viagem.");

		// Get the title of the book to buy as a start-up argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			localDeDestino = (String) args[0];
			System.out.println("Quero ir para "+localDeDestino);

			// Add a TickerBehaviour that schedules a request to seller agents every minute
			addBehaviour(new TickerBehaviour(this, 60000) {
				protected void onTick() {
					System.out.println("Quero ir para "+localDeDestino);
					// Update the list of seller agents
					DFAgentDeion template = new DFAgentDeion();
					ServiceDeion sd = new ServiceDeion();
					sd.setType("realizar-viagem");
					template.addServices(sd);
					try {
						DFAgentDeion[] result = DFService.search(myAgent, template); 
						System.out.println("Motoristas encontrados:");
						motoristasAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							motoristasAgents[i] = result[i].getName();
							System.out.println(motoristasAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Perform the request
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}
		else {
			// Make the agent terminate
			System.out.println("Destino não informado");
			doDelete();
		}
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Passageiro "+getAID().getName()+" realizou viagem.");
	}

	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by Book-buyer agents to request seller 
	   agents the target book.
	 */
	private class RequestPerformer extends Behaviour {
		private AID motoristaProximo; // The agent who provides the best offer 
		private int menorPreco;  // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < motoristasAgents.length; ++i) {
					cfp.addReceiver(motoristasAgents[i]);
				} 
				cfp.setContent(localDeDestino);
				cfp.setConversationId("corrida");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("corrida"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						int distancia = Integer.parseInt(reply.getContent());
						if (motoristaProximo == null || distancia < menorPreco) {
							// This is the best offer at present
							menorPreco = distancia;
							motoristaProximo = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= motoristasAgents.length) {
						// We received all replies
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(motoristaProximo);
				order.setContent(localDeDestino);
				order.setConversationId("corrida");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("corrida"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println("Motorista " +reply.getSender().getName() +" encontrado para " + localDeDestino);
						System.out.println("Preço = "+ menorPreco);
						myAgent.doDelete();
					}
					else {
						System.out.println("Nenhum táxi disponível");
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
				System.out.println("Nenhum táxi disponível para esse local");
			}
			return ((step == 2 && motoristaProximo == null) || step == 4);
		}
	}  // End of inner class RequestPerformer
}