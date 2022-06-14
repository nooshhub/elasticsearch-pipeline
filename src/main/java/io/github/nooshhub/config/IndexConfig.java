/*
 * Copyright 2021-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.nooshhub.config;

/**
 * Index configuration for init index script and sync sql.
 *
 * @author Neal Shan
 * @since 6/11/2022
 */
public class IndexConfig {

	private String indexName;

	// index settings and mappings
	private String indexSettingsPath;

	private String indexMappingPath;

	// sql
	private String initSql;

	private String syncSql;

	private String deleteSql;

	private String extensionSql;

	private String idColumns;

	private String extensionColumn;

	public String getIndexName() {
		return this.indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getIndexSettingsPath() {
		return this.indexSettingsPath;
	}

	public void setIndexSettingsPath(String indexSettingsPath) {
		this.indexSettingsPath = indexSettingsPath;
	}

	public String getIndexMappingPath() {
		return this.indexMappingPath;
	}

	public void setIndexMappingPath(String indexMappingPath) {
		this.indexMappingPath = indexMappingPath;
	}

	public String getInitSql() {
		return this.initSql;
	}

	public void setInitSql(String initSql) {
		this.initSql = initSql;
	}

	public String getSyncSql() {
		return this.syncSql;
	}

	public void setSyncSql(String syncSql) {
		this.syncSql = syncSql;
	}

	public String getDeleteSql() {
		return this.deleteSql;
	}

	public void setDeleteSql(String deleteSql) {
		this.deleteSql = deleteSql;
	}

	public String getExtensionSql() {
		return this.extensionSql;
	}

	public void setExtensionSql(String extensionSql) {
		this.extensionSql = extensionSql;
	}

	public String getIdColumns() {
		return this.idColumns;
	}

	public void setIdColumns(String idColumns) {
		this.idColumns = idColumns;
	}

	public String getExtensionColumn() {
		return this.extensionColumn;
	}

	public void setExtensionColumn(String extensionColumn) {
		this.extensionColumn = extensionColumn;
	}

}
