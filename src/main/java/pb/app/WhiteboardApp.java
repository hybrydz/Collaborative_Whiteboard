package pb.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pb.WhiteboardServer;
import pb.managers.ClientManager;
import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;
import pb.utils.Utils;


/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());
	
	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";
	
	/**
	 * White board map from board name to board object 
	 */
	Map<String,Whiteboard> whiteboards;
	
	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;

	/**
	 * adad
	 */
	Endpoint serverEndpoint = null ;
	PeerManager peerManager = null ;
	
	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport="standalone"; // a default value for the non-distributed version


	private static int pPort;
	private static int serverPort;
	private static String serverHost;

	volatile boolean flag = true;

	private static Map<String,Set<Endpoint>> peerListenList = new HashMap<>();

	private static ArrayList<Endpoint> endpointList = new ArrayList<>();

	
	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */
	
	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;
	
	/**
	 * Initialize the white board app.
	 */
	public WhiteboardApp(int peerPort,String whiteboardServerHost, 
			int whiteboardServerPort) {
		whiteboards=new HashMap<>();
		pPort = peerPort;
		serverPort = whiteboardServerPort;
		serverHost = whiteboardServerHost ;
		

		peerManager = new PeerManager(pPort);
		
		try{
			//P1
			peerManager.on(PeerManager.peerStarted, (eventArgs) -> {
				Endpoint endpoint = (Endpoint) eventArgs[0];
				log.info("Peer started:" + endpoint.getOtherEndpointId());
				endpoint.on(listenBoard,(eventArgs2)->{
					String boardName = (String)eventArgs2[0];
					log.info("Received listening request: "+boardName+" by "+endpoint.getOtherEndpointId());
					addListener(endpoint, boardName);
				}).on(getBoardData, (eventArgs2)->{
					String name = (String)eventArgs2[0];
					if(whiteboards.containsKey(name)){
						String data = whiteboards.get(name).toString();
						endpoint.emit(boardData, data);
						log.info("Sending boardData: ");
					}
				}).on(boardPathUpdate, (eventArgs2)->{
					System.out.println("Inside P1 boardPathUpdate");
					String data = (String)eventArgs2[0];
					String updatePath = getBoardPaths(data);
					long version = getBoardVersion(data) - 1;
					Whiteboard remoteBoard = whiteboards.get(getBoardName(data));
					if(remoteBoard.addPath(new WhiteboardPath(updatePath), version)){
						drawSelectedWhiteboard();
						if (!remoteBoard.isRemote()){
							String name = remoteBoard.getName();
							String nameVersion = remoteBoard.getNameAndVersion();
							if(peerListenList.containsKey(name)){
								Set<Endpoint> endpointList = peerListenList.get(name); 
								for(Endpoint e: endpointList){
									if(e !=endpoint){
										e.emit(boardPathUpdate,nameVersion+"%"+updatePath);
									}
									else{
										log.info("Not sending to endpoint "+endpoint.getOtherEndpointId());
									}
									
								}
							}
							endpoint.emit(boardPathAccepted, data);
						}
					}
					else{
						endpoint.emit(boardError, "Update Path error");
					}
				}).on(boardPathAccepted, (eventArgs2)->{ 
						log.info("Path accepted by peer");	
				}).on(boardUndoUpdate, (eventArgs2)->{
					String data = (String)eventArgs2[0];
					long version = getBoardVersion(data) - 1;
					Whiteboard remoteBoard = whiteboards.get(getBoardName(data));
					if(remoteBoard.undo(version)){
						drawSelectedWhiteboard();
						if(!remoteBoard.isRemote()){
							String name = remoteBoard.getName();
							String nameVersion = remoteBoard.getNameAndVersion();
							if(peerListenList.containsKey(name)){
								Set<Endpoint> endpointList = peerListenList.get(name); 
								for(Endpoint e: endpointList){
									if(e != endpoint){
										e.emit(boardUndoUpdate,nameVersion);
									}
									
								}
							}
							endpoint.emit(boardUndoAccepted, data);
						}
					}
					else{
						endpoint.emit(boardError, "Undo Path error");
					}
				}).on(boardUndoAccepted, (eventArgs2)->{ 
					log.info("Path Undo accepted by peer");
				}).on(boardClearUpdate, (eventArgs2)->{
					String data = (String)eventArgs2[0];
					long version = getBoardVersion(data) - 1;
					Whiteboard remoteBoard = whiteboards.get(getBoardName(data));
					if(remoteBoard.clear(version)){
						drawSelectedWhiteboard();
						if(!remoteBoard.isRemote()){
							String name = remoteBoard.getName();
							String nameVersion = remoteBoard.getNameAndVersion();
							if(peerListenList.containsKey(name)){
								Set<Endpoint> endpointList = peerListenList.get(name); 
								for(Endpoint e: endpointList){
									if(e!=endpoint){
										e.emit(boardClearUpdate,nameVersion);
									}
									
								}
							}
							endpoint.emit(boardClearAccepted, data);
						}
					}
					else{
						endpoint.emit(boardError, "Clear Board error");
					}
					
				}).on(boardClearAccepted, (eventArgs2)->{ 
					log.info("Path Clear accepted by peer");
				}).on(unlistenBoard, (eventArgs2)->{
					String boardName = (String)eventArgs2[0];
					log.info("Board Deleted : "+boardName+" by "+endpoint.getOtherEndpointId());
					deleteListener(endpoint,boardName);
				}).on(boardDeleted, (eventArgs2)->{
					String boardName = (String)eventArgs2[0];
					log.info("Board Deleted : "+boardName+" by "+endpoint.getOtherEndpointId());
					deleteBoard(boardName);
				}).on(boardError, (eventArgs2)->{
					String msg = (String)eventArgs2[0];
					log.info(msg+" "+endpoint.getOtherEndpointId());
				});				
				
			}).on(PeerManager.peerStopped, (eventArgs) -> {
				Endpoint endpoint = (Endpoint) eventArgs[0];
				log.info("Peer ended:" + endpoint.getOtherEndpointId());
			}).on(PeerManager.peerError, (eventArgs) -> {
				Endpoint endpoint = (Endpoint) eventArgs[0];
				log.info("Peer error:" + endpoint.getOtherEndpointId());
			}).on(PeerManager.peerServerManager, (eventArgs)->{
				ServerManager serverManager = (ServerManager)eventArgs[0];
				serverManager.on(IOThread.ioThread, (eventArgs2)->{
					String peerport = (String) eventArgs2[0];
					// we don't need this info, but let's log it
					this.peerport = peerport ;
				});
			});
			peerManager.start();

		ClientManager clientManager = peerManager.connect(serverPort, serverHost);
		clientManager.on(ClientManager.sessionStarted,(eventArgs)->{
        	serverEndpoint = (Endpoint)eventArgs[0];
			log.info("Peer started: "+serverEndpoint.getOtherEndpointId());

        	serverEndpoint.on(WhiteboardServer.sharingBoard,(eventArgs2)->{
				String name = (String)eventArgs2[0];
				log.info("Sharing: "+name);
				String ip = getIP(name);
				int port = getPort(name);
				log.info(ip+" "+port);
				try{
					//P2
					ClientManager peerClient = peerManager.connect(port, ip);
					
					
					peerClient.on(PeerManager.peerStarted,(eventArgs3)->{
						
						Endpoint peerEndpoint = (Endpoint)eventArgs3[0];
						endpointList.add(peerEndpoint);
						peerEndpoint.emit(listenBoard, name);
						log.info("Listening to: "+name);
						peerEndpoint.emit(getBoardData,name);
						log.info("Requesting Board Data: "+name);
						peerEndpoint.on(boardData, (eventArgs4)->{
							String data = (String)eventArgs4[0];
							log.info("Received board Data:");
							if(!whiteboards.containsKey(getBoardName(data))){
								Whiteboard sharedBoard = new Whiteboard(name, true);
								sharedBoard.whiteboardFromString(getBoardName(data), getBoardData(data));
								addBoard(sharedBoard, true);
								drawSelectedWhiteboard();
							}
							else{
								whiteboards.get(getBoardName(data)).whiteboardFromString(getBoardName(data), getBoardData(data));
								drawSelectedWhiteboard();
							}
						}).on(boardPathUpdate, (eventArgs4)->{
							System.out.println("Inside P2 boardPathUpdate");
							String data = (String)eventArgs4[0];
							String updatePath = getBoardPaths(data);
							long version = getBoardVersion(data) - 1;
							Whiteboard remoteBoard = whiteboards.get(getBoardName(data));
							if(remoteBoard.addPath(new WhiteboardPath(updatePath), version)){
								drawSelectedWhiteboard();
								if (!remoteBoard.isRemote()){
									String nameVersion = remoteBoard.getNameAndVersion();
									if(peerListenList.containsKey(remoteBoard.getName())){
										Set<Endpoint> endpointList = peerListenList.get(remoteBoard.getName()); 
										for(Endpoint e: endpointList){
											if(e !=peerEndpoint){
												e.emit(boardPathUpdate,nameVersion+"%"+updatePath);
											}
											else{
												log.info("Not sending to endpoint "+e.getOtherEndpointId());
											}
											
										}
									}
									peerEndpoint.emit(boardPathAccepted, data);
								}
							}
						}).on(boardUndoUpdate, (eventArgs4)->{
							String data = (String)eventArgs4[0];
							long version = getBoardVersion(data) - 1;
							Whiteboard remoteBoard = whiteboards.get(getBoardName(data));
							if(remoteBoard.undo(version)){
								drawSelectedWhiteboard();
								if(!remoteBoard.isRemote()){
									String nameVersion = remoteBoard.getNameAndVersion();
									if(peerListenList.containsKey(remoteBoard.getName())){
										Set<Endpoint> endpointList = peerListenList.get(remoteBoard.getName()); 
										for(Endpoint e: endpointList){
											if(e != peerEndpoint){
												e.emit(boardUndoUpdate,nameVersion);
											}
											else{
												log.info("Not sending to endpoint "+e.getOtherEndpointId());
											}
											
										}
									}
									peerEndpoint.emit(boardUndoAccepted, data);
								}
							}
						}).on(boardClearUpdate, (eventArgs4)->{
							String data = (String)eventArgs4[0];
							long version = getBoardVersion(data) - 1;
							Whiteboard remoteBoard = whiteboards.get(getBoardName(data));
							if(remoteBoard.clear(version)){
								drawSelectedWhiteboard();
								if(!remoteBoard.isRemote()){
									String nameVersion = remoteBoard.getNameAndVersion();
									if(peerListenList.containsKey(remoteBoard.getName())){
										Set<Endpoint> endpointList = peerListenList.get(remoteBoard.getName()); 
										for(Endpoint e: endpointList){
											if(e!=peerEndpoint){
												e.emit(boardClearUpdate,nameVersion);
											}
											
										}
									}
									peerEndpoint.emit(boardClearAccepted, data);
								}
							}
							else{
								peerEndpoint.emit(boardError, "Clear Board Update error");
							}
						}).on(boardDeleted, (eventArgs4)->{
							String boardName = (String)eventArgs2[0];
							log.info("Board Deleted : "+boardName+" by "+peerEndpoint.getOtherEndpointId());
							deleteBoard(boardName);
						
						}).on(boardError, (eventArgs4)->{
							String msg = (String)eventArgs4[0];
							log.info(msg+" "+peerEndpoint.getOtherEndpointId());
						});
					}).on(PeerManager.peerStopped, (eventArgs3)->{
						Endpoint peerEndpoint = (Endpoint)eventArgs3[0];
						endpointList.remove(peerEndpoint);
					});
					peerClient.start();
				}
				catch(Exception e){

				}
			}).on(WhiteboardServer.unsharingBoard,(eventArgs2)->{
				String name = (String)eventArgs2[0];
				log.info("Unsharing: "+name);
				try{
					// deleteBoard(name);
					whiteboards.remove(name);
					updateComboBox(null);
				}
				catch(Exception e){

				}
			});
        }).on(ClientManager.sessionStopped, (eventArgs)->{
			Endpoint endpoint= (Endpoint)eventArgs[0];
			log.info("Peer ended:"+endpoint.getOtherEndpointId());
		});

		clientManager.start();

		show(this.peerport);

		}
		catch(Exception e){

		}
	}
	
	/******
	 * 
	 * Utility methods to extract fields from argument strings.
	 * 
	 ******/
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}
	
	/******
	 * 
	 * Methods called from events.
	 * 
	 ******/
	
	// From whiteboard server
	
	public void addListener(Endpoint endpoint, String boardName){
		Set<Endpoint> listenerEndpoint = new HashSet<>();

		if(peerListenList.containsKey(boardName)){
			listenerEndpoint = peerListenList.get(boardName);
			if(!listenerEndpoint.contains(endpoint)){
				listenerEndpoint.add(endpoint);
				System.out.println(boardName+" listener added"+endpoint.getOtherEndpointId());
			}
		}
		else{
			listenerEndpoint.add(endpoint);
			peerListenList.put(boardName, listenerEndpoint);
			System.out.println(boardName+" listener and boardname added"+endpoint.getOtherEndpointId());
		}

	}

	public void deleteListener(Endpoint endpoint, String boardName){
		Set<Endpoint> listenerEndpoint = new HashSet<>();
		if(peerListenList.containsKey(boardName)){
			listenerEndpoint = peerListenList.get(boardName);
			if(listenerEndpoint.contains(endpoint)){
				listenerEndpoint.remove(endpoint);
				System.out.println(boardName+" Listener Removed "+endpoint.getOtherEndpointId());
			}
		}
	}
	

	public String boardData(String name){
		String data = "" ;
		data = whiteboards.get(name).toString();
		return data;
	}
	
	// From whiteboard peer
	
	
	
	/******
	 * 
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 * 
	 ******/
	
	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		while(this.flag){

		}
		System.out.println("Ended : "+flag);
	}
	
	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}
	
	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				whiteboards.remove(boardname);
				if(whiteboard.isRemote()){
					String name = whiteboard.getName();
					String ipPort = "/"+getIP(name)+":"+getPort(name);
					for(Endpoint e:endpointList){
						if(e.getOtherEndpointId().equals(ipPort)){
							e.emit(unlistenBoard,name);
							break;
						}
					}
				}
				else if (whiteboard.isShared()){
					String name = whiteboard.getName();
					if(peerListenList.containsKey(name)){
						Set<Endpoint> endpointList = peerListenList.get(name); 
						for(Endpoint e: endpointList){
							e.emit(boardDeleted,name);
						}
					}
				}
				
			}
			
		}
		updateComboBox(null);
	}
	
	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}
	
	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if(selectedBoard!=null) {
			if(!selectedBoard.addPath(currentPath,selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard(); // just redraw the screen without the path
			} else {
				// was accepted locally, so do remote stuff if needed
				if(selectedBoard.isRemote()){
					String name = selectedBoard.getName();
					String ipPort = "/"+getIP(name)+":"+getPort(name);
					String nameVersion = selectedBoard.getNameAndVersion();
					for(Endpoint e:endpointList){
						if(e.getOtherEndpointId().equals(ipPort)){
							e.emit(boardPathUpdate,nameVersion+"%"+currentPath);
							break;
						}
					}
				}
				else if (selectedBoard.isShared()){
					String name = selectedBoard.getName();
					String nameVersion = selectedBoard.getNameAndVersion();
					if(peerListenList.containsKey(name)){
						Set<Endpoint> endpointList = peerListenList.get(name); 
						for(Endpoint e: endpointList){
							e.emit(boardPathUpdate,nameVersion+"%"+currentPath);
						}
					}
				}
				else{
					//not shared
				}
				
			}
		} else {
			log.severe("path created without a selected board: "+currentPath);
		}
	}
	
	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				// was accepted locally, so do remote stuff if needed
				if(selectedBoard.isRemote()){
					String name = selectedBoard.getName();
					String ipPort = "/"+getIP(name)+":"+getPort(name);
					String nameVersion = selectedBoard.getNameAndVersion();
					for(Endpoint e:endpointList){
						if(e.getOtherEndpointId().equals(ipPort)){
							e.emit(boardClearUpdate,nameVersion);
							break;
						}	
					}
				}
				else if (selectedBoard.isShared()){
					String name = selectedBoard.getName();
					String nameVersion = selectedBoard.getNameAndVersion();
					if(peerListenList.containsKey(name)){
						Set<Endpoint> endpointList = peerListenList.get(name); 
						for(Endpoint e: endpointList){
							e.emit(boardClearUpdate,nameVersion);
						}
					}
				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("cleared without a selected board");
		}
	}
	
	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				if(selectedBoard.isRemote()){
					String name = selectedBoard.getName();
					String ipPort = "/"+getIP(name)+":"+getPort(name);
					String nameVersion = selectedBoard.getNameAndVersion();
					
					for(Endpoint e:endpointList){
						if(e.getOtherEndpointId().equals(ipPort)){
							e.emit(boardUndoUpdate,nameVersion);
							break;
						}	
					}
				}
				else if (selectedBoard.isShared()){
					String name = selectedBoard.getName();
					String nameVersion = selectedBoard.getNameAndVersion();
					if(peerListenList.containsKey(name)){
						Set<Endpoint> endpointList = peerListenList.get(name); 
						for(Endpoint e: endpointList){
							e.emit(boardUndoUpdate,nameVersion);
						}
					}
				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("undo without a selected board");
		}
	}
	
	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		drawSelectedWhiteboard();
		log.info("selected board: "+selectedBoard.getName());
	}
	
	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if(selectedBoard!=null) {
			selectedBoard.setShared(share);
			if(share){
				try{
					serverEndpoint.emit(WhiteboardServer.shareBoard, selectedBoard.getName());
				}
				catch(Exception e){

				}
			}
			else{
				try{
					serverEndpoint.emit(WhiteboardServer.unshareBoard, selectedBoard.getName());
				}
				catch(Exception e){

				}
			}
			
        } else {
        	log.severe("there is no selected board");
        }
	}
	
	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// do some final cleanup
		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});
    	whiteboards.values().forEach((whiteboard)->{
    	
		});
		
		peerManager.shutdown();
		this.flag = false;
		
	}
	
	

	/******
	 * 
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 * 
	 ******/
	
	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
			selectedBoard.draw(drawArea);
		}
	}
	
	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};
		
		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {    
	         public void itemStateChanged(ItemEvent e) { 
	            if(!modifyingCheckBox) setShare(e.getStateChange()==1);
	         }    
	      }); 
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
		

		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);
		
		
		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);
		
		// create an initial board
		createBoard();
		
		// closing the application
		frame.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure you want to close this window?", "Close Window?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
		        {
		        	guiShutdown();
		            frame.dispose();
		        }
		    }
		});
		
		// show the swing paint result
		frame.setVisible(true);
		
	}
	
	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 * 
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null && 
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						} 
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}
				
			}
		});
	}
	
}
