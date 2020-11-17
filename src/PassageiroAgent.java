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

}