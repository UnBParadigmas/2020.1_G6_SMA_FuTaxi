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
	/**
	 * 
	 */
	private static final long serialVersionUID = 3211228974125503848L;

	// The localDeAtuacao of books for sale (maps the title of a book to its price)
	private Hashtable localDeAtuacao;

	// The GUI by means of which the user can add books in the localDeAtuacao
	private MotoristaGui motoristaGui;

	// Put agent initializations here
	protected void setup() {
		// Create the localDeAtuacao
		localDeAtuacao = new Hashtable();

		// Create and show the GUI
		motoristaGui = new MotoristaGui(this);
		motoristaGui.showGui();

		// Register the book-selling service in the yellow pages
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

		// Add the behaviour serving queries from buyer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Close the GUI
		motoristaGui.dispose();
		// Printout a dismissal message
		System.out.println("Motorista "+getAID().getName()+" pronto.");
	}

	/**
     This is invoked by the GUI when the user adds a new book for sale
	 */
	public void atualizarLocaisDeAtuacao(final String local, final int preco) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				localDeAtuacao.put(local, new Integer(preco));
				System.out.println("Viagem para " + local+". Pre�o = "+preco);
			}
		} );
	}

	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String local = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer preco = (Integer) localDeAtuacao.get(local);
				if (preco != null) {
					// The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(preco.intValue()));
				}
				else {
					// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String local = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer preco = (Integer) localDeAtuacao.remove(local);
				if (preco != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println("Viagem para " +local+" realizada. Passageiro "+msg.getSender().getName());
				}
				else {
					// The requested book has been sold to another buyer in the meanwhile .
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer
}
