package filter;

import static util.ObjectUtil.redirect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;

import connection.SingleConnection;
import dao.DAOVersionadorBancoRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter(urlPatterns = { "/principal/*" })
public class FilterAutenticacao implements Filter {

	private Connection connection;

	@Override
	public void destroy() {
		try {
			if (connection != null)
				connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		try {
			HttpServletRequest req = (HttpServletRequest) request;
			HttpSession session = req.getSession();
			String usuarioLogado = (String) session.getAttribute("usuario");
			String urlParaAutenticar = req.getServletPath();

			if (usuarioLogado == null && !urlParaAutenticar.equalsIgnoreCase("/principal/ServletLogin")) {
				redirect((HttpServletRequest) request, (HttpServletResponse) response,
						"/index.jsp?url=" + urlParaAutenticar, Map.of("msg", "Favor, realize o login!"));
			} else {
				chain.doFilter(request, response);
			}
			connection.commit();
		} catch (Exception e) {
			e.printStackTrace();
			redirect((HttpServletRequest) request, (HttpServletResponse) response, "erro.jsp",
					Map.of("msg", e.getMessage()));
			try {
				connection.rollback();
			} catch (SQLException sqlException) {
				sqlException.printStackTrace();
			}
		}

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		connection = SingleConnection.getConnection();

		DAOVersionadorBancoRepository daoVersionador = new DAOVersionadorBancoRepository();

		String pathSQL = filterConfig.getServletContext().getRealPath("versionadorbancosql") + File.separator;

		var filesSQL = new File(pathSQL).listFiles();

		try {
			for (File file : filesSQL) {
				boolean arquivoExecutado = daoVersionador.arquivoSQLExecutado(file.getName());
				if (!arquivoExecutado) {
					try (var fileInputStream = new FileInputStream(file);
						 var scanner = new Scanner(fileInputStream, StandardCharsets.UTF_8)) {
						
						StringBuilder sql = new StringBuilder();
						while (scanner.hasNext()) {
							sql.append(scanner.nextLine()).append("\n");
						}
						connection.prepareStatement(sql.toString()).execute();
						daoVersionador.gravarArquivoSQLExecutado(file.getName());
						connection.commit();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				connection.rollback();
			} catch (SQLException sqlException) {
				sqlException.printStackTrace();
			}
		}
	}

}
