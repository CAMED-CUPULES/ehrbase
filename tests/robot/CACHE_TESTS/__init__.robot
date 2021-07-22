# Copyright (c) 2020 Jake Smolka (Hannover Medical School), Wladislaw Wagner (Vitasystems GmbH).
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



*** Settings ***
Metadata    Version    0.1.0
Metadata    Authors    *Wladislaw Wagner
Metadata    Created    2021.07.13
Metadata    Updated    2021.07.13

Documentation       Tests related to caching \n\n
...                 How to run these tests locally \n\n
...                 ============================== \n\n
...
...                 robot -d results/ -L TRACE robot/CACHE_TESTS/
...              OR
...                 robot -v SUT:ADMIN-TEST -d results/ -L TRACE robot/CACHE_TESTS/
...              OR (when you have started EHRbase + DB manually)
...                 robot -v SUT:ADMIN-DEV -d results/ -L TRACE robot/CACHE_TESTS/


Force Tags    CACHE
