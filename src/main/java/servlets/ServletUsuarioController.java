package servlets;

import static java.util.Objects.isNull;
import static util.ConstantsUtil.ACAO;
import static util.ConstantsUtil.MSG;
import static util.ConstantsUtil.REDIRECT_USUARIO;
import static util.ConstantsUtil.TOTAL_PAGINA;
import static util.ConstantsUtil.USUARIOS;
import static util.ObjectUtil.isObjectValid;
import static util.ObjectUtil.redirect;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.jakartaee.commons.compress.utils.IOUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.databind.ObjectMapper;

import dao.DAOUsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.ModelLogin;

@MultipartConfig
@WebServlet(urlPatterns = {"/ServletUsuarioController"})
public class ServletUsuarioController extends ServletGenericUtil {
	
	private static final long serialVersionUID = 1L;
	
	private DAOUsuarioRepository daoUsuarioRepository = new DAOUsuarioRepository();

    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	try {
    		var msg = "";
	    	String acao = request.getParameter(ACAO);
	    	int totalPagina = daoUsuarioRepository.getTotalPaginas(getUserLogado(request).getId());
	    	if (isObjectValid(acao) && acao.equals("deletar")) {
	    		String id = request.getParameter("id");
	    		daoUsuarioRepository.deletarUsuario(id);
	    		if (acao.equalsIgnoreCase("deletar")) {
		    		msg = "Excluído com sucesso!";
		    		List<ModelLogin> usuarios = daoUsuarioRepository.listarTodos(getUserLogado(request).getId());
		    		redirect(request, response, REDIRECT_USUARIO, Map.of(MSG, msg, USUARIOS, usuarios, TOTAL_PAGINA, totalPagina));
		    	} else if (acao.equals("deletar-ajax")) {
		    		response.getWriter().write("Excluído com sucesso!");
		    	} 
	    	} else if (isObjectValid(acao) && acao.equals("buscarUsuario-ajax")) {
	    		String nomeBusca = request.getParameter("nomeBusca");
	    		var dadosJsonUsuario = daoUsuarioRepository.buscarPorNome(nomeBusca, getUserLogado(request).getId());
	    		var json = new ObjectMapper().writeValueAsString(dadosJsonUsuario);
	    		response.addHeader("totalPagina", "" + daoUsuarioRepository.getTotalPaginasPorNome(nomeBusca, getUserLogado(request).getId()));
	    		response.getWriter().write(json);
	    	} else if (isObjectValid(acao) && acao.equals("buscarUsuario-ajax-page")) {
	    		String nomeBusca = request.getParameter("nomeBusca");
	    		String pagina = request.getParameter("pagina");
	    		var dadosJsonUsuario = daoUsuarioRepository.buscarPorNomePaginado(nomeBusca, getUserLogado(request).getId(), pagina);
	    		var json = new ObjectMapper().writeValueAsString(dadosJsonUsuario);
	    		response.addHeader("totalPagina", "" + daoUsuarioRepository.getTotalPaginasPorNome(nomeBusca, getUserLogado(request).getId()));
	    		response.getWriter().write(json);
	    	} else if (isObjectValid(acao) && acao.equals("buscarEditar")) {
	    		msg = "Usuário em edição!";
	    		String id = request.getParameter("id");
	    		var modelLogin = daoUsuarioRepository.buscarPoId(id, getUserLogado(request).getId());
	    		List<ModelLogin> usuarios = daoUsuarioRepository.listarTodos(getUserLogado(request).getId());
	    		redirect(request, response, REDIRECT_USUARIO, Map.of("modelLogin", modelLogin, MSG, msg, USUARIOS, usuarios, TOTAL_PAGINA, totalPagina));
	    	} else if (isObjectValid(acao) && acao.equals("listar")) {
	    		msg = "Usuários carregados!";
	    		List<ModelLogin> usuarios = daoUsuarioRepository.listarTodos(getUserLogado(request).getId());
	    		redirect(request, response, REDIRECT_USUARIO, Map.of(USUARIOS, usuarios, MSG, msg, TOTAL_PAGINA, totalPagina));
	    	} else if (isObjectValid(acao) && acao.equals("downloadFoto")) {
	    		String id = request.getParameter("id");
	    		var modelLogin = daoUsuarioRepository.buscarPoId(id, getUserLogado(request).getId());
	    		if (isObjectValid(modelLogin.getFoto())) {
	    			response.setHeader("Content-Disposition", "attachment;filename=arquivo." + modelLogin.getExtensaoFoto());
	    			response.getOutputStream().write(Base64.decodeBase64(modelLogin.getFoto().split("\\,")[1]));
	    		}
	    	} else if(isObjectValid(acao) && acao.equals("paginar")) {
	    		Integer offset = Integer.parseInt(request.getParameter("pagina"));
	    		List<ModelLogin> usuarios = daoUsuarioRepository.listarTodosPaginado(getUserLogado(request).getId(), offset);
	    		redirect(request, response, REDIRECT_USUARIO, Map.of(MSG, msg, USUARIOS, usuarios, TOTAL_PAGINA, totalPagina));
	    	} else {
	    		List<ModelLogin> usuarios = daoUsuarioRepository.listarTodos(getUserLogado(request).getId());
	    		redirect(request, response, REDIRECT_USUARIO, Map.of(MSG, msg, USUARIOS, usuarios, TOTAL_PAGINA, totalPagina));
	    	}	    
    	} catch(Exception e) {
    		e.printStackTrace();
			redirect(request, response, "erro.jsp", Map.of(MSG, e.getMessage()));
    	}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		try {
			var msg = "";
			int totalPagina = daoUsuarioRepository.getTotalPaginas(getUserLogado(request).getId());
			String id = request.getParameter("id");
			String nome = request.getParameter("nome");
			String email = request.getParameter("email");
			String login = request.getParameter("login");
			String senha = request.getParameter("senha");
			String perfil = request.getParameter("perfil");
			String sexo = request.getParameter("sexo");
			String dataNascimento = request.getParameter("dataNascimento");
			String rendaMensal = request.getParameter("rendaMensal");
			
			String[] endereco = {
				request.getParameter("cep"),
				request.getParameter("logradouro"),
				request.getParameter("bairro"),
				request.getParameter("localidade"),
				request.getParameter("uf"),
				request.getParameter("numero")
			};			
			
			var modelLogin = new ModelLogin(
				isObjectValid(id) ? Long.parseLong(id) : null, 
				nome, email, login, senha, perfil, sexo,
				Date.valueOf(new SimpleDateFormat("yyyy-mm-dd").format(new SimpleDateFormat("dd/mm/yyyy").parse(dataNascimento))),
				Double.parseDouble(rendaMensal.split("\\ ")[1].replaceAll("\\.", "").replaceAll("\\,", "."))
			);
			
			preencherEndereco(modelLogin, endereco);
			
			var part = request.getPart("fileFoto");
			if (ServletFileUpload.isMultipartContent(request) && part.getSize() > 0) {
				var extensao = part.getContentType().split("\\/")[1];
				byte[] foto = IOUtils.toByteArray(part.getInputStream());
				var base64 = "data:image/" + extensao + ";base64," + Base64.encodeBase64String(foto);
				modelLogin.setFoto(base64);
				modelLogin.setExtensaoFoto(extensao);
			}
			
			if (daoUsuarioRepository.validarLogin(modelLogin.getLogin()) && isNull(modelLogin.getId())) {
				msg = "Já existe usuário com o mesmo login, informe outro login!";
			} else {
				msg = modelLogin.isNovo() ? "Gravado com sucesso!" : "Atualizado com sucesso!";
				modelLogin = daoUsuarioRepository.gravarUsuario(modelLogin, getUserLogado(request).getId());
			}
			
			List<ModelLogin> usuarios = daoUsuarioRepository.listarTodos(getUserLogado(request).getId());
			redirect(request, response, REDIRECT_USUARIO, Map.of("modelLogin", modelLogin, MSG, msg, USUARIOS, usuarios, TOTAL_PAGINA, totalPagina));
		} catch (Exception e) {
			e.printStackTrace();
			redirect(request, response, "erro.jsp", Map.of(MSG, e.getMessage()));
		}
	}
	
	private void preencherEndereco(ModelLogin modelLogin, String... dados) throws SQLException {
		modelLogin.getEndereco().setCep(dados[0]);
		modelLogin.getEndereco().setLogradouro(dados[1]);
		modelLogin.getEndereco().setBairro(dados[2]);
		modelLogin.getEndereco().setLocalidade(dados[3]);
		modelLogin.getEndereco().setUf(dados[4]);
		modelLogin.getEndereco().setNumero(dados[5]);
	}

}
